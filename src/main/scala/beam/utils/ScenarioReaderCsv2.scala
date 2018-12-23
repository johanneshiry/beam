package beam.utils

import beam.sim.BeamServices
import com.typesafe.scalalogging.LazyLogging
import org.matsim.api.core.v01.Id
import org.matsim.api.core.v01.population.Person
import org.matsim.core.population.PopulationUtils
import org.matsim.core.scenario.MutableScenario
import org.matsim.households.{Household, HouseholdIncomeComparator}

import scala.collection.mutable.ListBuffer

class ScenarioReaderCsv2(var scenario: MutableScenario, var beamServices: BeamServices, val delimiter: String = ",")
  extends LazyLogging {

  val scenarioFolder = beamServices.beamConfig.beam.agentsim.agents.population.beamPopulationDirectory

  private val defaultAvailableModes =
    "car,ride_hail,bike,bus,funicular,gondola,cable_car,ferry,tram,transit,rail,subway,tram"

  val buildingFilePath = scenarioFolder + "/buildings.csv"
  val personFilePath = scenarioFolder + "/persons.csv"
  val householdFilePath = scenarioFolder + "/households.csv"

  val planFilePath = scenarioFolder + "/plans.csv"
  val unitFilePath = scenarioFolder + "/units.csv"
  val parcelAttrFilePath = scenarioFolder + "/parcel_attr.csv"

  def loadScenario() = {

    scenario.getPopulation.getPersons.clear()
    scenario.getPopulation.getPersonAttributes.clear()
    scenario.getHouseholds.getHouseholds.clear()
    scenario.getHouseholds.getHouseholdAttributes.clear()
    beamServices.privateVehicles.clear()
    /////

    logger.info("Reading units...")
    val units = BeamServices.readUnitsFile(unitFilePath)

    logger.info("Reading parcel attrs")
    val parcelAttrs = BeamServices.readParcelAttrFile(parcelAttrFilePath)

    logger.info("Reading Buildings...")
    val buildings = BeamServices.readBuildingsFile(buildingFilePath)

    logger.info("Reading Persons...")
    val persons = BeamServices.readPersonsFile(personFilePath, scenario.getPopulation, defaultAvailableModes)

    logger.info("Reading plans...")
    BeamServices.readPlansFile(planFilePath, scenario.getPopulation)

    logger.info("In case a person is not having a corresponding plan entry, just adding a dummy empty plan")

    val listOfPersonsWithoutPlan: ListBuffer[Person] = ListBuffer()
    scenario.getPopulation.getPersons.forEach {
      case (pk: Id[Person], pv: Person) => {
        if(pv.getSelectedPlan == null){
          /*val plan = PopulationUtils.createPlan(pv)
          pv.addPlan(plan)
          pv.setSelectedPlan(plan)*/
          listOfPersonsWithoutPlan += pv
        }
      }
    }

    println("Total persons " + scenario.getPopulation.getPersons.size())
    println("Total Persons Persons without plan " + listOfPersonsWithoutPlan.size)
    listOfPersonsWithoutPlan.take(100).map(p => println(p))
    listOfPersonsWithoutPlan.foreach{
      p => {
        scenario.getPopulation.removePerson(p.getId)
      }
    }


    logger.info("Reading Households...")

    BeamServices.readHouseHoldsFile(
      householdFilePath,
      scenario,
      beamServices,
      persons.par,
      units.par,
      buildings.par,
      parcelAttrs.par
    )

    val listOfHouseHoldsWithoutMembers: ListBuffer[Household] = ListBuffer()
    scenario.getHouseholds.getHouseholds.forEach{
      case(hId: Id[Household], h: Household) => {
        if(h.getMemberIds.size() == 0){
          listOfHouseHoldsWithoutMembers += h
        }
      }
    }

    logger.info("Removing households without members " + listOfHouseHoldsWithoutMembers.size)
    listOfHouseHoldsWithoutMembers.take(100).map( l => println(l) )
    listOfHouseHoldsWithoutMembers.foreach{
      h =>
        scenario.getHouseholds.getHouseholdAttributes.removeAllAttributes(h.getId.toString)
        scenario.getHouseholds.getHouseholds.remove(h.getId)
    }

    /*val houseHolds = BeamServices.readHouseHoldsFile(householdFilePath, scenario, beamServices,
      TrieMap[Id[Household], ListBuffer[Id[Person]]](),
      TrieMap[String, java.util.Map[String, String]](), TrieMap[String, java.util.Map[String, String]](), TrieMap[String, java.util.Map[String, String]]())*/

  }

}