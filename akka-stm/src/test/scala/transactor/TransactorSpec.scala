package akka.transactor.test

import org.scalatest.WordSpec
import org.scalatest.matchers.MustMatchers

import akka.transactor.Transactor
import akka.actor.{Actor, ActorRef}
import akka.stm._
import akka.util.duration._

import java.util.concurrent.CountDownLatch

object TransactorIncrement {
  case class Increment(friends: Seq[ActorRef], latch: CountDownLatch)
  case object GetCount

  class Counter(name: String) extends Transactor {
    val count = Ref(0)

    override def transactionFactory = TransactionFactory(timeout = 3 seconds)

    def increment = {
      log.info(name + ": incrementing")
      count alter (_ + 1)
    }

    override def coordinate = {
      case Increment(friends, latch) => {
        if (friends.nonEmpty) sendTo(friends.head -> Increment(friends.tail, latch))
        else nobody
      }
    }

    override def before = {
      case i: Increment => log.info(name + ": before transaction")
    }

    def atomically = {
      case Increment(friends, latch) => {
        increment
        deferred { latch.countDown }
        compensating { latch.countDown }
      }
    }

    override def after = {
      case i: Increment => log.info(name + ": after transaction")
    }

    override def normally = {
      case GetCount => self.reply(count.get)
    }
  }

  class Failer extends Transactor {
    def atomically = {
      case _ => throw new RuntimeException("Expected failure")
    }
  }
}

class TransactorSpec extends WordSpec with MustMatchers {
  import TransactorIncrement._

  val numCounters = 5
  val timeout = 5 seconds

  def createTransactors = {
    def createCounter(i: Int) = Actor.actorOf(new Counter("counter" + i)).start
    val counters = (1 to numCounters) map createCounter
    val failer = Actor.actorOf(new Failer).start
    (counters, failer)
  }

  "Transactor increment" should {
    "increment all counters by one with successful transactions" in {
      val (counters, failer) = createTransactors
      val incrementLatch = new CountDownLatch(numCounters)
      counters(0) ! Increment(counters.tail, incrementLatch)
      incrementLatch.await(timeout.length, timeout.unit)
      for (counter <- counters) {
        (counter !! GetCount).get must be === 1
      }
      counters foreach (_.stop)
      failer.stop
    }

    "increment no counters with a failing transaction" in {
      val (counters, failer) = createTransactors
      val failLatch = new CountDownLatch(numCounters + 1)
      counters(0) ! Increment(counters.tail :+ failer, failLatch)
      failLatch.await(timeout.length, timeout.unit)
      for (counter <- counters) {
        (counter !! GetCount).get must be === 0
      }
      counters foreach (_.stop)
      failer.stop
    }
  }
}
