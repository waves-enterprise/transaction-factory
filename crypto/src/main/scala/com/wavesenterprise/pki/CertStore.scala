package com.wavesenterprise.pki

import cats.implicits.{catsStdInstancesForEither, catsSyntaxFlatMapOps}
import com.wavesenterprise.crypto.internals.{CryptoError, PKIError}
import com.wavesenterprise.utils.ReadWriteLocking

import java.security.cert.X509Certificate
import java.util.concurrent.locks.{ReadWriteLock, ReentrantReadWriteLock}
import javax.security.auth.x500.X500Principal
import scala.annotation.tailrec
import scala.collection.mutable

/**
  * CertStore with certificate chains represented as a mutable single-directed graph data structure.
  * Chains without common certificates are represented as disjoint graphs.
  * Graph consistency implemented with Kahn's algorithm.
  * Self-signed certificates are taken as CA certificates.
  */
class CertStore private (
    private val caCerts: mutable.Set[X500Principal],
    private val clientCerts: mutable.Set[X500Principal],
    private val certsByDN: mutable.Map[X500Principal, X509Certificate],
    private val certsGraph: mutable.Map[X500Principal, mutable.Set[X500Principal]]
) extends ReadWriteLocking {
  override protected val lock: ReadWriteLock = new ReentrantReadWriteLock()

  def getCaCerts: Set[X500Principal]     = readLock(caCerts.toSet)
  def getClientCerts: Set[X500Principal] = readLock(clientCerts.toSet)

  /**
    * @return Certificates chain ordered from client to CA certificate.
    */
  def getCertChain(userCert: X509Certificate): Either[CryptoError, CertChain] =
    readLock {
      val subject = userCert.getSubjectX500Principal
      if (clientCerts.contains(subject)) {
        val chain = buildChain(subject)
        Right(CertChain(chain.last, chain.tail.init, chain.head))
      } else if (certsByDN.contains(subject)) {
        Left(PKIError(s"Unable to build cert chain starting from the intermediate cert '$subject'"))
      } else {
        Left(PKIError(s"Certificate '$subject' was not found in the CertChain"))
      }
    }

  @tailrec
  private def buildChain(certDn: X500Principal, acc: List[X509Certificate] = List.empty): Seq[X509Certificate] = {
    val cert    = certsByDN(certDn)
    val nextAcc = cert :: acc
    if (caCerts.contains(certDn)) {
      nextAcc.view.reverse.toIndexedSeq
    } else {
      buildChain(cert.getIssuerX500Principal, nextAcc)
    }
  }

  /**
    * Adds a new certificate into CertStore.
    * All the intermediate certificates must be already in the CertStore.
    * The issuer must be intermediate or CA certificate.
    */
  def addCert(cert: X509Certificate): Either[CryptoError, Unit] =
    writeLock {
      val subject = cert.getSubjectX500Principal
      if (certsByDN.contains(subject)) {
        Right(())
      } else {
        val issuer = cert.getIssuerX500Principal
        (if (issuer == subject) {
           caCerts.add(subject)
           Right(())
         } else {
           Either.cond(
             !clientCerts.contains(issuer),
             (),
             PKIError(s"Certificate '$subject' is issued by a client certificate '$issuer' which is forbidden")
           ) >> certsByDN
             .get(issuer)
             .toRight(PKIError(s"Issuer's certificate '$issuer' was not found in the CertStore"))
             .flatMap { _ =>
               val nei = certsGraph.getOrElseUpdate(issuer, mutable.HashSet.empty)
               Either
                 .cond(
                   !nei.contains(subject), {
                     certsGraph.put(subject, mutable.HashSet.empty)
                     clientCerts.add(subject)
                     nei.add(subject)
                   },
                   PKIError(s"Duplicated certificate connection '$issuer' <- '$subject'")
                 )
             }
         }).map(_ => certsByDN.put(cert.getSubjectX500Principal, cert))
      }
    }

  /**
    * Adds all the certificates from the input collection.
    * Complete chains including intermediate and CA certificates must be passed within the input certificates.
    */
  def addCertificates(certs: List[X509Certificate]): Either[CryptoError, Unit] = {
    CertStore.buildCertStore(certs, certsByDN.clone()).map(writeLock(mergeWith))
  }

  /**
    * Removes certificate form the CertStore.
    * The issuer's certificates will be removed in case no linked issued certificates remain.
    */
  def removeCert(cert: X509Certificate): Either[CryptoError, Unit] =
    writeLock {
      val subject = cert.getSubjectX500Principal
      if (certsByDN.contains(subject)) {
        Either.cond(
          certsGraph(subject).isEmpty,
          removeCertChain(cert),
          PKIError(s"Unable to remove intermediate certificate '$subject' which still has dependent certificates")
        )
      } else {
        Right(())
      }
    }

  @tailrec
  private def removeCertChain(cert: X509Certificate): Unit = {
    val subject = cert.getSubjectX500Principal
    if (certsGraph(subject).isEmpty) {
      certsByDN.remove(subject)
      certsGraph.remove(subject)
      val issuer = cert.getIssuerX500Principal
      certsGraph.get(issuer).foreach { issuersCerts =>
        issuersCerts.remove(subject)
      }
      if (subject == issuer) {
        caCerts.remove(subject)
      } else {
        clientCerts.remove(subject)
        removeCertChain(certsByDN(issuer))
      }
    }
  }

  private def mergeWith(other: CertStore): Unit = {
    this.caCerts ++= other.caCerts
    this.clientCerts ++= other.clientCerts
    this.certsByDN ++= other.certsByDN
    other.certsGraph.foreach {
      case (root, neighbours) => this.certsGraph.getOrElseUpdate(root, mutable.Set.empty) ++= neighbours
    }
  }
}

object CertStore {
  def fromCertificates(certs: List[X509Certificate]): Either[CryptoError, CertStore] = {
    val certsByDN = mutable.HashMap.empty[X500Principal, X509Certificate]
    buildCertStore(certs, certsByDN)
  }

  private def buildCertStore(
      newCerts: List[X509Certificate],
      certsByDN: mutable.Map[X500Principal, X509Certificate]
  ): Either[CryptoError, CertStore] = {
    val certsCount = newCerts.size
    val adjMap     = mutable.HashMap.empty[X500Principal, mutable.Set[X500Principal]]
    val inDegree   = mutable.HashMap.empty[X500Principal, Int]

    @tailrec
    def buildGraph(certIterator: Iterator[X509Certificate]): Either[CryptoError, Unit] = {
      if (certIterator.hasNext) {
        val cert    = certIterator.next()
        val issuer  = cert.getIssuerX500Principal
        val subject = cert.getSubjectX500Principal

        (if (!certsByDN.contains(subject)) {
           certsByDN.put(subject, cert)
           if (issuer != subject) {
             adjMap.getOrElseUpdate(subject, mutable.HashSet.empty[X500Principal])
             val nei = adjMap.getOrElseUpdate(issuer, mutable.HashSet.empty[X500Principal])
             Either.cond(
               !nei.contains(subject),
               nei.add(subject),
               PKIError(s"Duplicated certificate connection: '$issuer' <- '$subject'")
             )
           } else {
             Right(())
           }
         } else {
           Right(())
         }) match {
          case Right(_) =>
            if (!inDegree.contains(subject)) {
              if (issuer == subject) {
                inDegree.getOrElseUpdate(subject, 0)
              } else {
                inDegree.put(subject, inDegree.getOrElseUpdate(subject, 0) + 1)
              }
            }
            buildGraph(certIterator)
          case Left(err) => Left(err)
        }
      } else {
        Right(())
      }
    }

    def findRootCerts(): Either[PKIError, Set[X500Principal]] = {
      val (rootCerts, errorCerts) = inDegree
        .collect {
          case (dn, count) if count == 0 => dn
        }
        .partition { dn =>
          val cert = certsByDN(dn)
          cert.getIssuerX500Principal == cert.getSubjectX500Principal
        }
      Either.cond(errorCerts.isEmpty, rootCerts.toSet, PKIError(s"Couldn't build cert chain for the following certificates: $errorCerts"))
    }

    def validateGraph(rootCerts: Set[X500Principal]): Either[CryptoError, Unit] = {
      val queue = mutable.Queue.empty[X500Principal]
      rootCerts.foreach(queue.enqueue(_))
      var visitedCount = 0
      while (queue.nonEmpty) {
        val curr = queue.dequeue()
        if (inDegree(curr) == 0) {
          visitedCount += 1
        }
        adjMap.get(curr).foreach { neighbours =>
          neighbours.foreach { nei =>
            inDegree.put(nei, inDegree(nei) - 1)
            if (inDegree(nei) == 0) {
              queue.enqueue(nei)
            }
          }
        }
      }
      Either.cond(
        visitedCount == certsCount,
        (), {
          val failedCerts = inDegree.collect {
            case (dn, count) if count > 0 => dn
          }
          PKIError(s"Unable to build cert chain for the following certificates: [${failedCerts.mkString(", ")}]")
        }
      )
    }

    def createCertStore(rootCerts: Set[X500Principal]): Either[CryptoError, CertStore] = {
      val clientCerts = adjMap
        .collect {
          case (dn, neighbours) if neighbours.isEmpty => dn
        }
        .to[mutable.Set]
      Right(new CertStore(rootCerts.to[mutable.Set], clientCerts, certsByDN, adjMap))
    }

    for {
      _         <- buildGraph(newCerts.iterator)
      rootCerts <- findRootCerts()
      _         <- validateGraph(rootCerts)
      certStore <- createCertStore(rootCerts)
    } yield certStore
  }
}

case class CertChain(caCert: X509Certificate, intermediateCerts: Seq[X509Certificate], userCert: X509Certificate)
