/**
 * Copyright (C) 2009-2010 Scalable Solutions AB <http://scalablesolutions.se>
 */

package se.scalablesolutions.akka.config

import se.scalablesolutions.akka.actor.{ActorRef}
import se.scalablesolutions.akka.dispatch.MessageDispatcher

sealed abstract class FaultHandlingStrategy {
  def trapExit: List[Class[_ <: Throwable]]
}

object AllForOneStrategy {
  def apply(trapExit: List[Class[_ <: Throwable]], maxNrOfRetries: Int, withinTimeRange: Int) =
    new AllForOneStrategy(trapExit, if (maxNrOfRetries < 0) None else Some(maxNrOfRetries), if (withinTimeRange < 0) None else Some(withinTimeRange))
  def apply(trapExit: Array[Class[Throwable]], maxNrOfRetries: Int, withinTimeRange: Int) =
    new AllForOneStrategy(trapExit.toList,maxNrOfRetries,withinTimeRange)
}

case class AllForOneStrategy(trapExit: List[Class[_ <: Throwable]],
                             maxNrOfRetries: Option[Int] = None,
                             withinTimeRange: Option[Int] = None) extends FaultHandlingStrategy {
  def this(trapExit: List[Class[_ <: Throwable]],maxNrOfRetries: Int, withinTimeRange: Int) =
    this(trapExit, if (maxNrOfRetries < 0) None else Some(maxNrOfRetries), if (withinTimeRange < 0) None else Some(withinTimeRange))
  def this(trapExit: Array[Class[Throwable]],maxNrOfRetries: Int, withinTimeRange: Int) =
    this(trapExit.toList,maxNrOfRetries,withinTimeRange)
}

object OneForOneStrategy {
  def apply(trapExit: List[Class[_ <: Throwable]], maxNrOfRetries: Int, withinTimeRange: Int) =
    new OneForOneStrategy(trapExit, if (maxNrOfRetries < 0) None else Some(maxNrOfRetries), if (withinTimeRange < 0) None else Some(withinTimeRange))
  def apply(trapExit: Array[Class[Throwable]], maxNrOfRetries: Int, withinTimeRange: Int) =
    new OneForOneStrategy(trapExit.toList,maxNrOfRetries,withinTimeRange)
}

case class OneForOneStrategy(trapExit: List[Class[_ <: Throwable]],
                             maxNrOfRetries: Option[Int] = None,
                             withinTimeRange: Option[Int] = None) extends FaultHandlingStrategy {
  def this(trapExit: List[Class[_ <: Throwable]],maxNrOfRetries: Int, withinTimeRange: Int) =
    this(trapExit, if (maxNrOfRetries < 0) None else Some(maxNrOfRetries), if (withinTimeRange < 0) None else Some(withinTimeRange))
  def this(trapExit: Array[Class[Throwable]],maxNrOfRetries: Int, withinTimeRange: Int) =
    this(trapExit.toList,maxNrOfRetries,withinTimeRange)
}

case object NoFaultHandlingStrategy extends FaultHandlingStrategy {
  def trapExit: List[Class[_ <: Throwable]] = Nil
}

/**
 * Configuration classes - not to be used as messages.
 *
 * @author <a href="http://jonasboner.com">Jonas Bon&#233;r</a>
 */
object ScalaConfig {
  sealed abstract class ConfigElement

  abstract class Server extends ConfigElement
  abstract class FailOverScheme extends ConfigElement
  abstract class LifeCycle extends ConfigElement

  case class SupervisorConfig(restartStrategy: RestartStrategy, worker: List[Server]) extends Server {
  //Java API
    def this(restartStrategy: RestartStrategy, worker: Array[Server]) = this(restartStrategy,worker.toList)
  }

  class Supervise(val actorRef: ActorRef, val lifeCycle: LifeCycle, _remoteAddress: RemoteAddress) extends Server {
    val remoteAddress: Option[RemoteAddress] = if (_remoteAddress eq null) None else Some(_remoteAddress)
  }
  
  object Supervise {
    def apply(actorRef: ActorRef, lifeCycle: LifeCycle, remoteAddress: RemoteAddress) = new Supervise(actorRef, lifeCycle, remoteAddress)
    def apply(actorRef: ActorRef, lifeCycle: LifeCycle) = new Supervise(actorRef, lifeCycle, null)
    def unapply(supervise: Supervise) = Some((supervise.actorRef, supervise.lifeCycle, supervise.remoteAddress))
  }

  case class RestartStrategy(
      scheme: FailOverScheme,
      maxNrOfRetries: Int,
      withinTimeRange: Int,
      trapExceptions: List[Class[_ <: Throwable]]) extends ConfigElement

  case object AllForOne extends FailOverScheme
  case object OneForOne extends FailOverScheme

  case object Permanent extends LifeCycle
  case object Temporary extends LifeCycle
  case object UndefinedLifeCycle extends LifeCycle

  case class RemoteAddress(val hostname: String, val port: Int) extends ConfigElement

  class Component(_intf: Class[_],
                  val target: Class[_],
                  val lifeCycle: LifeCycle,
                  val timeout: Long,
                  val transactionRequired: Boolean,
                  _dispatcher: MessageDispatcher, // optional
                  _remoteAddress: RemoteAddress   // optional
          ) extends Server {
    val intf: Option[Class[_]] = if (_intf eq null) None else Some(_intf)
    val dispatcher: Option[MessageDispatcher] = if (_dispatcher eq null) None else Some(_dispatcher)
    val remoteAddress: Option[RemoteAddress] = if (_remoteAddress eq null) None else Some(_remoteAddress)
  }
  object Component {
    def apply(intf: Class[_], target: Class[_], lifeCycle: LifeCycle, timeout: Long) =
      new Component(intf, target, lifeCycle, timeout, false, null, null)

    def apply(target: Class[_], lifeCycle: LifeCycle, timeout: Long) =
      new Component(null, target, lifeCycle, timeout, false, null, null)

    def apply(intf: Class[_], target: Class[_], lifeCycle: LifeCycle, timeout: Long, dispatcher: MessageDispatcher) =
      new Component(intf, target, lifeCycle, timeout, false, dispatcher, null)

    def apply(target: Class[_], lifeCycle: LifeCycle, timeout: Long, dispatcher: MessageDispatcher) =
      new Component(null, target, lifeCycle, timeout, false, dispatcher, null)

    def apply(intf: Class[_], target: Class[_], lifeCycle: LifeCycle, timeout: Long, remoteAddress: RemoteAddress) =
      new Component(intf, target, lifeCycle, timeout, false, null, remoteAddress)

    def apply(target: Class[_], lifeCycle: LifeCycle, timeout: Long, remoteAddress: RemoteAddress) =
      new Component(null, target, lifeCycle, timeout, false, null, remoteAddress)

    def apply(intf: Class[_], target: Class[_], lifeCycle: LifeCycle, timeout: Long, dispatcher: MessageDispatcher, remoteAddress: RemoteAddress) =
      new Component(intf, target, lifeCycle, timeout, false, dispatcher, remoteAddress)

    def apply(target: Class[_], lifeCycle: LifeCycle, timeout: Long, dispatcher: MessageDispatcher, remoteAddress: RemoteAddress) =
      new Component(null, target, lifeCycle, timeout, false, dispatcher, remoteAddress)

    def apply(intf: Class[_], target: Class[_], lifeCycle: LifeCycle, timeout: Long, transactionRequired: Boolean) =
      new Component(intf, target, lifeCycle, timeout, transactionRequired, null, null)

    def apply(target: Class[_], lifeCycle: LifeCycle, timeout: Long, transactionRequired: Boolean) =
      new Component(null, target, lifeCycle, timeout, transactionRequired, null, null)

    def apply(intf: Class[_], target: Class[_], lifeCycle: LifeCycle, timeout: Long, transactionRequired: Boolean, dispatcher: MessageDispatcher) =
      new Component(intf, target, lifeCycle, timeout, transactionRequired, dispatcher, null)

    def apply(target: Class[_], lifeCycle: LifeCycle, timeout: Long, transactionRequired: Boolean, dispatcher: MessageDispatcher) =
      new Component(null, target, lifeCycle, timeout, transactionRequired, dispatcher, null)

    def apply(intf: Class[_], target: Class[_], lifeCycle: LifeCycle, timeout: Long, transactionRequired: Boolean, remoteAddress: RemoteAddress) =
      new Component(intf, target, lifeCycle, timeout, transactionRequired, null, remoteAddress)

    def apply(target: Class[_], lifeCycle: LifeCycle, timeout: Long, transactionRequired: Boolean, remoteAddress: RemoteAddress) =
      new Component(null, target, lifeCycle, timeout, transactionRequired, null, remoteAddress)

    def apply(intf: Class[_], target: Class[_], lifeCycle: LifeCycle, timeout: Long, transactionRequired: Boolean, dispatcher: MessageDispatcher, remoteAddress: RemoteAddress) =
      new Component(intf, target, lifeCycle, timeout, transactionRequired, dispatcher, remoteAddress)

    def apply(target: Class[_], lifeCycle: LifeCycle, timeout: Long, transactionRequired: Boolean, dispatcher: MessageDispatcher, remoteAddress: RemoteAddress) =
      new Component(null, target, lifeCycle, timeout, transactionRequired, dispatcher, remoteAddress)
  }
}

/**
 * @author <a href="http://jonasboner.com">Jonas Bon&#233;r</a>
 */
object JavaConfig {
  import scala.reflect.BeanProperty

  sealed abstract class ConfigElement

  class RestartStrategy(
      @BeanProperty val scheme: FailOverScheme,
      @BeanProperty val maxNrOfRetries: Int,
      @BeanProperty val withinTimeRange: Int,
      @BeanProperty val trapExceptions: Array[Class[_ <: Throwable]]) extends ConfigElement {
    def transform = se.scalablesolutions.akka.config.ScalaConfig.RestartStrategy(
      scheme.transform, maxNrOfRetries, withinTimeRange, trapExceptions.toList)
  }

  abstract class LifeCycle extends ConfigElement {
    def transform: se.scalablesolutions.akka.config.ScalaConfig.LifeCycle
  }

  class Permanent extends LifeCycle {
    override def transform = se.scalablesolutions.akka.config.ScalaConfig.Permanent
  }
  
  class Temporary extends LifeCycle {
    override def transform = se.scalablesolutions.akka.config.ScalaConfig.Temporary
  }

  class UndefinedLifeCycle extends LifeCycle {
    override def transform = se.scalablesolutions.akka.config.ScalaConfig.UndefinedLifeCycle
  }

  abstract class FailOverScheme extends ConfigElement {
    def transform: se.scalablesolutions.akka.config.ScalaConfig.FailOverScheme
  }
  class AllForOne extends FailOverScheme {
    override def transform = se.scalablesolutions.akka.config.ScalaConfig.AllForOne
  }
  class OneForOne extends FailOverScheme {
    override def transform = se.scalablesolutions.akka.config.ScalaConfig.OneForOne
  }

  class RemoteAddress(@BeanProperty val hostname: String, @BeanProperty val port: Int)

  abstract class Server extends ConfigElement
  class Component(@BeanProperty val intf: Class[_],
                  @BeanProperty val target: Class[_],
                  @BeanProperty val lifeCycle: LifeCycle,
                  @BeanProperty val timeout: Long,
                  @BeanProperty val transactionRequired: Boolean,  // optional
                  @BeanProperty val dispatcher: MessageDispatcher, // optional
                  @BeanProperty val remoteAddress: RemoteAddress   // optional
          ) extends Server {

    def this(intf: Class[_], target: Class[_], lifeCycle: LifeCycle, timeout: Long) =
      this(intf, target, lifeCycle, timeout, false, null, null)

    def this(target: Class[_], lifeCycle: LifeCycle, timeout: Long) =
      this(null, target, lifeCycle, timeout, false, null, null)

    def this(intf: Class[_], target: Class[_], lifeCycle: LifeCycle, timeout: Long, remoteAddress: RemoteAddress) =
      this(intf, target, lifeCycle, timeout, false, null, remoteAddress)

    def this(target: Class[_], lifeCycle: LifeCycle, timeout: Long, remoteAddress: RemoteAddress) =
      this(null, target, lifeCycle, timeout, false, null, remoteAddress)

    def this(intf: Class[_], target: Class[_], lifeCycle: LifeCycle, timeout: Long, dispatcher: MessageDispatcher) =
      this(intf, target, lifeCycle, timeout, false, dispatcher, null)

    def this(target: Class[_], lifeCycle: LifeCycle, timeout: Long, dispatcher: MessageDispatcher) =
      this(null, target, lifeCycle, timeout, false, dispatcher, null)

    def this(target: Class[_], lifeCycle: LifeCycle, timeout: Long, dispatcher: MessageDispatcher, remoteAddress: RemoteAddress) =
      this(null, target, lifeCycle, timeout, false, dispatcher, remoteAddress)

    def this(intf: Class[_], target: Class[_], lifeCycle: LifeCycle, timeout: Long, transactionRequired: Boolean) =
      this(intf, target, lifeCycle, timeout, transactionRequired, null, null)

    def this(target: Class[_], lifeCycle: LifeCycle, timeout: Long, transactionRequired: Boolean) =
      this(null, target, lifeCycle, timeout, transactionRequired, null, null)

    def this(intf: Class[_], target: Class[_], lifeCycle: LifeCycle, timeout: Long, transactionRequired: Boolean, remoteAddress: RemoteAddress) =
      this(intf, target, lifeCycle, timeout, transactionRequired, null, remoteAddress)

    def this(target: Class[_], lifeCycle: LifeCycle, timeout: Long, transactionRequired: Boolean, remoteAddress: RemoteAddress) =
      this(null, target, lifeCycle, timeout, transactionRequired, null, remoteAddress)

    def this(intf: Class[_], target: Class[_], lifeCycle: LifeCycle, timeout: Long, transactionRequired: Boolean, dispatcher: MessageDispatcher) =
      this(intf, target, lifeCycle, timeout, transactionRequired, dispatcher, null)

    def this(target: Class[_], lifeCycle: LifeCycle, timeout: Long, transactionRequired: Boolean, dispatcher: MessageDispatcher) =
      this(null, target, lifeCycle, timeout, transactionRequired, dispatcher, null)

    def this(target: Class[_], lifeCycle: LifeCycle, timeout: Long, transactionRequired: Boolean, dispatcher: MessageDispatcher, remoteAddress: RemoteAddress) =
      this(null, target, lifeCycle, timeout, transactionRequired, dispatcher, remoteAddress)

    def transform =
      se.scalablesolutions.akka.config.ScalaConfig.Component(
        intf, target, lifeCycle.transform, timeout, transactionRequired, dispatcher,
        if (remoteAddress ne null) se.scalablesolutions.akka.config.ScalaConfig.RemoteAddress(remoteAddress.hostname, remoteAddress.port) else null)

    def newSupervised(actorRef: ActorRef) =
      se.scalablesolutions.akka.config.ScalaConfig.Supervise(actorRef, lifeCycle.transform)
  }

}
