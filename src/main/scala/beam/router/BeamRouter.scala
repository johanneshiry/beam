package beam.router

import akka.actor.Actor
import beam.agentsim.core.Modes.BeamMode
import beam.agentsim.core.Modes.BeamMode._
import beam.agentsim.events.SpaceTime
import beam.agentsim.sim.AgentsimServices
import org.matsim.api.core.v01.Coord
import org.slf4j.{Logger, LoggerFactory}

trait BeamRouter extends Actor {
  val log: Logger = LoggerFactory.getLogger(getClass)
}

object BeamRouter{

  case class BeamTrip(legs: Vector[BeamLeg], choiceUtility: Double = 0.0) {
    lazy val tripClassifier: BeamMode = if (legs map (_.mode) contains CAR) {
      CAR
    } else {
      TRANSIT
    }
    val totalTravelTime: Long = legs.map(_.travelTime).sum
  }

  object BeamTrip {
    val noneTrip: BeamTrip = BeamTrip(Vector[BeamLeg]())
  }

  case class BeamLeg(startTime: Long, mode: BeamMode, travelTime: Long, graphPath: BeamGraphPath)

  object BeamLeg {

    def dummyWalk(startTime: Long): BeamLeg = new BeamLeg(startTime, WALK, 0, BeamGraphPath.empty)

  }

  case class BeamGraphPath(linkIds: Vector[String],
                           latLons: Vector[Coord],
                           entryTimes: Vector[Long]) {

    lazy val trajectory: Vector[SpaceTime] = {
      latLons zip entryTimes map {
        SpaceTime(_)
      }
    }
  }

  object BeamGraphPath {
    val emptyTimes: Vector[Long] = Vector[Long]()
    val errorPoints: Vector[Coord] = Vector[Coord](new Coord(0.0, 0.0))
    val errorTime: Vector[Long] = Vector[Long](-1L)

    val empty: BeamGraphPath = new BeamGraphPath(Vector[String](), errorPoints, emptyTimes)


  }

  case class EdgeModeTime(fromVertexLabel: String, mode: BeamMode, time: Long, fromCoord: Coord, toCoord: Coord)

}

