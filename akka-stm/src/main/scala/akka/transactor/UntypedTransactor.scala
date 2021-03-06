/**
 * Copyright (C) 2009-2010 Scalable Solutions AB <http://scalablesolutions.se>
 */

package akka.transactor

import akka.actor.{UntypedActor, ActorRef}
import akka.stm.{DefaultTransactionConfig, TransactionFactory}

import java.util.{Set => JSet}

import scala.collection.JavaConversions._


/**
 * An UntypedActor version of transactor for using from Java.
 */
abstract class UntypedTransactor extends UntypedActor {
  private lazy val txFactory = transactionFactory

  /**
   * Create default transaction factory. Override to provide custom configuration.
   */
  def transactionFactory = TransactionFactory(DefaultTransactionConfig)

  /**
   * Implement a general pattern for using coordinated transactions.
   */
  @throws(classOf[Exception])
  final def onReceive(message: Any): Unit = {
    message match {
      case coordinated @ Coordinated(message) => {
        val others = coordinate(message)
        for (sendTo <- others) {
          sendTo.actor.sendOneWay(coordinated(sendTo.message.getOrElse(message)))
        }
        before(message)
        coordinated.atomic(txFactory) { atomically(message) }
        after(message)
      }
      case message => {
        val normal = normally(message)
        if (!normal) onReceive(Coordinated(message))
      }
    }
  }

  /**
   * Override this method to coordinate with other transactors.
   * The other transactors are added to the coordinated transaction barrier
   * and sent a Coordinated message. The message to send can be specified
   * or otherwise the same message as received is sent. Use the 'include' and
   * 'sendTo' methods to easily create the set of transactors to be involved.
   */
  @throws(classOf[Exception])
  def coordinate(message: Any): JSet[SendTo] = nobody

  /**
   * Empty set of transactors to send to.
   */
  def nobody: JSet[SendTo] = Set[SendTo]()

  /**
   * For including one other actor in this coordinated transaction and sending
   * them the same message as received. Use as the result in `coordinated`.
   */
  def include(actor: ActorRef): JSet[SendTo] = Set(SendTo(actor))

  /**
   * For including one other actor in this coordinated transaction and specifying the
   * message to send. Use as the result in `coordinated`.
   */
  def include(actor: ActorRef, message: Any): JSet[SendTo] =  Set(SendTo(actor, Some(message)))

  /**
   * For including another actor in this coordinated transaction and sending
   * them the same message as received. Use to create the result in `coordinated`.
   */
  def sendTo(actor: ActorRef): SendTo = SendTo(actor)

  /**
   * For including another actor in this coordinated transaction and specifying the
   * message to send. Use to create the result in `coordinated`.
   */
  def sendTo(actor: ActorRef, message: Any): SendTo =  SendTo(actor, Some(message))

  /**
   * A Receive block that runs before the coordinated transaction is entered.
   */
  @throws(classOf[Exception])
  def before(message: Any): Unit = {}

  /**
   * The Receive block to run inside the coordinated transaction.
   */
  @throws(classOf[Exception])
  def atomically(message: Any): Unit = {}

  /**
   * A Receive block that runs after the coordinated transaction.
   */
  @throws(classOf[Exception])
  def after(message: Any): Unit = {}

  /**
   * Bypass transactionality and behave like a normal actor.
   */
  @throws(classOf[Exception])
  def normally(message: Any): Boolean = false
}
