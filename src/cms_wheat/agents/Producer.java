package cms_wheat.agents;

import repast.simphony.essentials.RepastEssentials;
import repast.simphony.random.RandomHelper;
import cms_wheat.Cms_builder;
import cms_wheat.utils.ElementOfSupplyOrDemandCurve;
import cms_wheat.agents.MarketSession;
import cms_wheat.dynamics.Cms_scheduler;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Iterator;
/**
 * The Producer class hold all the relevant variable for a producer; It has methods for performing the producer's actions. The realization of production and the decision on the quantity offered in each market session are of particular importance.  
 * @author Gianfranco Giulioni
 *
 */
public class Producer {
	public String name,iso3Code,markets,varieties;
	public double latitude,longitude,productionShare,sizeInGuiDisplay;
	public boolean exportAllowed=true;
	public ArrayList<Double> supplyPrices=new ArrayList<Double>();
	public ArrayList<Integer> productionInputs=new ArrayList<Integer>();
	public ArrayList<Integer> productionInputs1=new ArrayList<Integer>();
	Iterator<Integer> productionInputsIterator,productionInputsIterator1;
	public ArrayList<Double> marketSessionsPricesRecord=new ArrayList<Double>();
	public ArrayList<ElementOfSupplyOrDemandCurve> supplyCurve=new ArrayList<ElementOfSupplyOrDemandCurve>();
	public ArrayList<MarketSession> marketSessionsList=new ArrayList<MarketSession>();
	public double priceEarnedInLatestMarketSession,quantitySoldInLatestMarketSession;
	public String varietySoldInLatestMarketSession;
	int timeOfFirstProduction=1;
	int timeOfFirstProductionSpring=1;
	int initialProduction,targetProduction,stock,numberOfMarkets,totalMarketSessions,remainingMarketSessions,offerInThisSession,production,production_winter,production_spring,toBeSoldInEachMarketSessionToExhaustStock;
	double sumOfSellingPrices,averageSellingPrice,fuelPrice,productionCosts,reservationPrice;
	double markUp=-0.8; //reservatioPrice=(1+markUp)*productionCosts
	double crudeOilBarrelPerNHectars=0.1;
	double fixUnitCost=5.0;
	double QuantityMultiplierToDecreaseReservationPrice=1.2;//QuantityMultiplierToDecreaseReservationPrice*toBeSoldInEachMarketSessionToExhaustStock
	double shareOfRemainingSessionsToDecreaseReservationPrice=0.5;
	double toleranceInReducingSupply=0.05; //if sold quantity is below the supply quantity by this percentage, reduce supply by the following parameter 
	double shareOfSupplyToBeReducedInCaseOfUnsoldProducts=0.05;
/**
 *The Cms_builder calls the constructor giving as parameters the values found in a line of the producers.csv file located in the data folder.
 *<br>
 *The format of each line is the following:
 *<br>
 *name,ISO3code,latitude,longitude,listOfMarkets,listOfProducedVarietyes,timeOfProduction,productionInputs
 *<br>
 *When the producer sells in more than one market, the market names are separated by the | character in the listOfMarkets
 *<br>
 *When the producer makes more than one variety, the varieties names are separated by the | character in the listOfProducedVarieties. However, the present version of the model does not handle multiple products and the user should modify the code to achieve this result.
 *<br>
 *Note that the timeOfProduction is not used by this constructor, but it is used by the setup method.
 *<br>
 *example:
 *<br>
 *China_mainland,CHN,36.6094323800447,103.865365256658,Shanghai|Mumbay,wheat,06,101587008,106390000,...
 *<br>
 *This line gives the geographic coordinates of China and says that this country sells in two markets, we consider the generic item wheat whose production is obtained in the sixth period (if periods corresponds to month, it realizes the production in June), the produced quantity in the fist considered period is 101587008, in the second period is 106390000 and so on.
 * @param producerName string
 * @param producerIso3Code string
 * @param producerLatitude double
 * @param producerLongitude double
 * @param producerMarkets string
 * @param producerVarieties string
 * @param producerProductionInputs arrayList of integers
 * @param possiblePrices arrayList of double
 */
	public Producer(String producerName,String producerIso3Code,double producerLatitude,double producerLongitude,String producerMarkets,String producerVarieties,ArrayList<Integer> producerProductionInputs,ArrayList<Integer> producerProductionInputs1,ArrayList<Double> possiblePrices){
		name=producerName;
		iso3Code=producerIso3Code;
		latitude=producerLatitude;
		longitude=producerLongitude;
		markets=producerMarkets;
		String[] partsTmpMarkets=markets.split("\\|");
		numberOfMarkets=partsTmpMarkets.length;
		varieties=producerVarieties;
		supplyPrices=possiblePrices;
		productionInputs=producerProductionInputs;
		productionInputsIterator=productionInputs.iterator();
		production_winter=productionInputsIterator.next();
		productionInputsIterator.remove();
		productionInputs1=producerProductionInputs1;
		productionInputsIterator1=productionInputs1.iterator();
		production_spring=productionInputsIterator1.next();
		productionInputsIterator1.remove();
		production=production_winter+production_spring;
//		productionShare=(double)productionInputs.get(0)/Cms_builder.globalProduction;
		productionShare=(double)production/Cms_builder.globalProduction;
		sizeInGuiDisplay=productionShare*20;
			if(sizeInGuiDisplay<8){
		sizeInGuiDisplay=8;
		}
		if(Cms_builder.verboseFlag){
			System.out.println("Created producer: "+name+", ISO3.code "+iso3Code+", latitude: "+latitude+", longitude: "+longitude);
			System.out.println("        sells in "+numberOfMarkets+" market(s): "+markets);
			System.out.println("        produces: "+varieties);
			System.out.println("        prod. inputs winter: current "+production_winter+" future "+producerProductionInputs);
			System.out.println("        prod. inputs spring: current "+production_spring+" future "+producerProductionInputs1);
			System.out.println("        prod. total: "+production);
		}
	}

	public void stepExportAllowedFlag(){
		if(RandomHelper.nextDouble()<Cms_builder.probabilityToAllowExport){
			exportAllowed=true;
		}
		else{
			exportAllowed=false;
		}

// start Russian ban
/*
		if(name.equals("Russian Federation")){
//Timing Russian Federation 
//periodo di raccolta luglio
//band da agosto a dicembre
//415 realizzazione produzione
//416 variazione quantita' messe in vendita
			if(RepastEssentials.GetTickCount()>415 && RepastEssentials.GetTickCount()<421){
				exportAllowed=false;
//System.out.println(RepastEssentials.GetTickCount()+" time "+name+" exportFlag: "+exportAllowed);
			}

		}
*/
// end Russian ban

		if(Cms_builder.verboseFlag){System.out.println("         producer:    "+name+" exportAllowed "+exportAllowed);}
	}

	public ArrayList<ElementOfSupplyOrDemandCurve> getSupplyCurve(String theVariety){
		offerInThisSession=(int)(production/12);
/*
		//if unsold quantity exists
		if(quantitySoldInLatestMarketSession*(1+toleranceInReducingSupply)<offerInThisSession){
			offerInThisSession+=-shareOfSupplyToBeReducedInCaseOfUnsoldProducts*offerInThisSession;
		}//if unsold quantity does not exist
		else{
			offerInThisSession=(int)stock/remainingMarketSessions;
		}
*/

/*
		if(offerInThisSession>QuantityMultiplierToDecreaseReservationPrice*toBeSoldInEachMarketSessionToExhaustStock && remainingMarketSessions<shareOfRemainingSessionsToDecreaseReservationPrice*totalMarketSessions ){
			reservationPrice=(new BigDecimal(reservationPrice-reservationPrice/remainingMarketSessions).setScale(3,RoundingMode.HALF_EVEN)).doubleValue();
		}
*/
//		reservationPrice=(new BigDecimal(reservationPrice-reservationPrice/remainingMarketSessions).setScale(3,RoundingMode.HALF_EVEN)).doubleValue();
//		System.out.println(name+" reservation price "+reservationPrice);
		supplyCurve=new ArrayList<ElementOfSupplyOrDemandCurve>();
		for(Double aPrice : supplyPrices){
			supplyCurve.add(new ElementOfSupplyOrDemandCurve(aPrice,(double)offerInThisSession));
		}

		fuelPrice=Cms_scheduler.crudeOilPrice;
		productionCosts=fixUnitCost+fuelPrice*crudeOilBarrelPerNHectars;
		reservationPrice=(new BigDecimal((1+markUp)*productionCosts).setScale(3,RoundingMode.HALF_EVEN)).doubleValue();
		
		if(Cms_builder.verboseFlag){System.out.println("           "+name+" stock "+stock+" remaining market sessions "+remainingMarketSessions);}
		if(Cms_builder.verboseFlag){System.out.println("           supply curve sent to market by "+name+" for product "+theVariety+" (some points)");}
		return supplyCurve;
	}
	public double getReservationPrice(){
		return reservationPrice;
	}
	public void setQuantitySoldInLatestMarketSession(String theVariety, double marketPrice, double soldQuantity){
		varietySoldInLatestMarketSession=theVariety;
		priceEarnedInLatestMarketSession=marketPrice;
		quantitySoldInLatestMarketSession=soldQuantity;
		marketSessionsPricesRecord.add(Double.valueOf(priceEarnedInLatestMarketSession));
		if(marketSessionsPricesRecord.size()>Cms_builder.producersPricesMemoryLength){
			marketSessionsPricesRecord.remove(0);
		}
			if(Cms_builder.verboseFlag){System.out.println("           "+name+" state before session stock: "+stock+" remaining sessions: "+remainingMarketSessions);}
			if(Cms_builder.verboseFlag){System.out.println("           "+name+" price "+priceEarnedInLatestMarketSession+" quantity sold in this session "+quantitySoldInLatestMarketSession+" of "+varietySoldInLatestMarketSession);}
			stock+=-quantitySoldInLatestMarketSession;
			remainingMarketSessions--;
			if(Cms_builder.verboseFlag){System.out.println("           "+name+" state after session stock: "+stock+" remaining sessions: "+remainingMarketSessions);}
	}
public void manageStock(){
		if(Cms_builder.verboseFlag){
			System.out.println(name+" manage Stock before winter production; stock "+stock);
		}
		stock=0;
		if(Cms_builder.verboseFlag){
			System.out.println(name+" Stock managed; stock "+stock);
		}

	}
	
	public void manageStockBeforeWinterProduction(){
		if(Cms_builder.verboseFlag){
			System.out.println(name+" manage Stock before winter production; stock "+stock);
		}
		if(timeOfFirstProductionSpring>=timeOfFirstProduction){
			if(stock>production_spring*(timeOfFirstProductionSpring-timeOfFirstProduction)/12){
				stock=production_spring*(timeOfFirstProductionSpring-timeOfFirstProduction)/12;
			}
		}
		else{
			if(stock>production_spring*(12+timeOfFirstProductionSpring-timeOfFirstProduction)/12){
				stock=production_spring*(12+timeOfFirstProductionSpring-timeOfFirstProduction)/12;
			}
		}
		if(Cms_builder.verboseFlag){
			System.out.println(name+" Stock managed; stock "+stock);
		}

	}
	public void makeProduction(){
			if(Cms_builder.verboseFlag){System.out.println(name+" state before winter production stock: "+stock+" remaining sessions: "+remainingMarketSessions);}

//we assume that the stock as absorbed by unconsidered uses
			//stock=0;
			production_winter=(Double.valueOf(production_winter*(1+(RandomHelper.nextDouble()*2-1.0)*Cms_builder.productionRateOfChangeControl))).intValue();
			stock+=production_winter;
			production=production_winter+production_spring;
/*
			if(RepastEssentials.GetTickCount()<150){
				production=(Double.valueOf(targetProduction*(1+(RandomHelper.nextDouble()*2-1.0)*0.05))).intValue();
				}
	*/		
			if(RepastEssentials.GetTickCount()>Cms_builder.startUsingInputsFromTimeTick && productionInputs.size()>0){
				if(Cms_builder.verboseFlag){System.out.println(name+" production taken from winter input record");}
				production_winter=productionInputsIterator.next();
				stock+=production_winter;
				production=production_winter+production_spring;
				targetProduction=production;
				productionInputsIterator.remove();
			}
/*			
			//satart annealing
			else{
				if((RepastEssentials.GetTickCount() % 100) <= 12 && (RepastEssentials.GetTickCount()/100)>1){
					production=(Double.valueOf(initialProduction*(1+(RandomHelper.nextDouble()*2-1.0)*0.2))).intValue();
					targetProduction=production;
					System.out.println(RepastEssentials.GetTickCount()+" annealing "+name+" "+production);
				}
				else{
					production=initialProduction;
					targetProduction=production;
				}
			}
			//end annealing
*/			
//reservation price setting moved to getSupplyCurve
			//fuelPrice=Cms_scheduler.crudeOilPrice;
			//productionCosts=fixUnitCost+fuelPrice*crudeOilBarrelPerNHectars;
			//reservationPrice=(new BigDecimal((1+markUp)*productionCosts).setScale(3,RoundingMode.HALF_EVEN)).doubleValue();
//			System.out.println(name+ " fuel price "+fuelPrice+" productionCosts "+productionCosts+" reservatioPrice "+reservationPrice);
			 
//			stock+=production;
			remainingMarketSessions=totalMarketSessions;
			toBeSoldInEachMarketSessionToExhaustStock=stock/remainingMarketSessions;
/*
			if(name.equals("Russian Federation")){
System.out.println(RepastEssentials.GetTickCount()+" time "+name+" production realized: "+production);
}
*/
		if(Cms_builder.verboseFlag){System.out.println(name+" winter production realized: "+production_winter+" stock "+stock);}
//System.out.println(name+" state after production stock: "+stock+" remaining sessions: "+remainingMarketSessions);
		//plan production for the next period
		if(marketSessionsPricesRecord.size()==Cms_builder.producersPricesMemoryLength){
			if(Cms_builder.verboseFlag){System.out.println(name+" set target production for next production cycle");}
			//compute average price
			sumOfSellingPrices=0;
			for(Double tmpDouble : marketSessionsPricesRecord){
				sumOfSellingPrices+=tmpDouble;
			}
			averageSellingPrice=sumOfSellingPrices/marketSessionsPricesRecord.size();
			//increase production if average selling price higher than threshold
			if(averageSellingPrice>Cms_builder.priceThresholdToIncreaseTargetProduction){
				targetProduction=(int)(targetProduction*(1+Cms_builder.percentageChangeInTargetProduction));
				if(Cms_builder.verboseFlag){System.out.println(name+" target production increased to "+targetProduction);}
			}
			//increase production if average selling price higher than threshold
			if(averageSellingPrice<Cms_builder.priceThresholdToDecreaseTargetProduction){
				targetProduction=(int)(targetProduction*(1+Cms_builder.percentageChangeInTargetProduction));
				if(Cms_builder.verboseFlag){System.out.println(name+" target production decreased to "+targetProduction);}
			}
		}
		else{
			averageSellingPrice=0;
			if(Cms_builder.verboseFlag){System.out.println(name+" there are not enough data to set target production for next production cycle");}
		}
		
	}
	
public void manageStockBeforeSpringProduction(){
		if(Cms_builder.verboseFlag){
			System.out.println(name+" manage Stock before winter production; stock "+stock);
		}
		if(timeOfFirstProduction>=timeOfFirstProductionSpring){
			if(stock>production_winter*(timeOfFirstProduction-timeOfFirstProductionSpring)/12){
				stock=production_winter*(timeOfFirstProduction-timeOfFirstProductionSpring)/12;
			}
		}
		else{
			if(stock>production_winter*(12+timeOfFirstProduction-timeOfFirstProductionSpring)/12){
				stock=production_winter*(12+timeOfFirstProduction-timeOfFirstProductionSpring)/12;
			}
		}
		if(Cms_builder.verboseFlag){
			System.out.println(name+" Stock managed; stock "+stock);
		}

	}
	


	public void makeProductionSpring(){
			if(Cms_builder.verboseFlag){System.out.println(name+" state before spring production stock: "+stock+" remaining sessions: "+remainingMarketSessions);}

//we assume that the stock as absorbed by unconsidered uses
			production_spring=(Double.valueOf(production_spring*(1+(RandomHelper.nextDouble()*2-1.0)*Cms_builder.productionRateOfChangeControl))).intValue();
			stock+=production_spring;
			production=production_winter+production_spring;
/*
			if(RepastEssentials.GetTickCount()<150){
				production=(Double.valueOf(targetProduction*(1+(RandomHelper.nextDouble()*2-1.0)*0.05))).intValue();
				}
	*/		
			if(RepastEssentials.GetTickCount()>Cms_builder.startUsingInputsFromTimeTick && productionInputs1.size()>0){
				if(Cms_builder.verboseFlag){System.out.println(name+" production taken from spring input record");}
				production_spring=productionInputsIterator1.next();
				stock+=production_spring;
				production=production_winter+production_spring;
				targetProduction=production;
				productionInputsIterator1.remove();
			}
			if(timeOfFirstProduction==0){
				remainingMarketSessions=totalMarketSessions;
				toBeSoldInEachMarketSessionToExhaustStock=stock/remainingMarketSessions;
			}

		if(Cms_builder.verboseFlag){System.out.println(name+" spring production realized: "+production+" stock "+stock);}

	}



	public void setup(int producerTimeOfFirstProduction,int producerTimeOfFirstProductionSpring){
		timeOfFirstProduction=producerTimeOfFirstProduction;
		timeOfFirstProductionSpring=producerTimeOfFirstProductionSpring;
		//	initialProduction=(int)(productionShare*Cms_builder.globalProduction);
		//	targetProduction=initialProduction;
		//	production=targetProduction;
		//	stock=(int)(productionShare*Cms_builder.globalProduction*((double)timeOfFirstProduction/Cms_builder.productionCycleLength));
		initialProduction=production;
		targetProduction=production;
		// stock=(int)(initialProduction*((double)timeOfFirstProduction/Cms_builder.productionCycleLength));
		stock=(int)(production_winter*((double)timeOfFirstProduction/Cms_builder.productionCycleLength)+production_spring*((double)timeOfFirstProductionSpring/Cms_builder.productionCycleLength));
		if(timeOfFirstProduction==0){
			stock=(int)(production_spring*((double)timeOfFirstProductionSpring/Cms_builder.productionCycleLength));
		}
		if(timeOfFirstProductionSpring==0){
			stock=(int)(production_winter*((double)timeOfFirstProduction/Cms_builder.productionCycleLength));
		}

		totalMarketSessions=numberOfMarkets*Cms_builder.productionCycleLength;
		remainingMarketSessions=numberOfMarkets*timeOfFirstProduction;
		if(timeOfFirstProduction==0){
			remainingMarketSessions=numberOfMarkets*timeOfFirstProductionSpring;
		}

		if(Cms_builder.verboseFlag){
			if(timeOfFirstProduction==0 || timeOfFirstProductionSpring==0){
				if(timeOfFirstProduction==0){
					if(timeOfFirstProductionSpring==0){
						System.out.println("        WARNING: no production ");
					}
					else{
						System.out.println("        no production of winter variety; time of First production spring "+timeOfFirstProductionSpring+" production "+(int)(productionShare*Cms_builder.globalProduction)+" stock "+stock+" total market sessions "+totalMarketSessions+" remaining market sessions "+remainingMarketSessions);
					}
				}
				else{
					if(timeOfFirstProduction==0){
						System.out.println("        WARNING: no production ");
					}
					else{
						System.out.println("        time of First production winter "+timeOfFirstProduction+"; no production of spring variety; production "+(int)(productionShare*Cms_builder.globalProduction)+" stock "+stock+" total market sessions "+totalMarketSessions+" remaining market sessions "+remainingMarketSessions);
					}
				}
			}
			else{
				System.out.println("        time of First production winter "+timeOfFirstProduction+"; time of First production spring "+timeOfFirstProductionSpring+" production "+(int)(productionShare*Cms_builder.globalProduction)+" stock "+stock+" total market sessions "+totalMarketSessions+" remaining market sessions "+remainingMarketSessions);
			}
		}
	}
	public void addMarketSession(MarketSession aNewMarketSession){
		marketSessionsList.add(aNewMarketSession);
		if(Cms_builder.verboseFlag){System.out.println("     producer "+name+" market Session added");}
	}
	public ArrayList<MarketSession> getMarkeSessions(){
		return marketSessionsList;
	}
	public String getName(){
		return name;
	}
	public String getIso3Code(){
		return iso3Code;
	}
	public String getMarkets(){
		return markets;
	}
	public String getVarieties(){
		return varieties;
	}
	public double getLatitude(){
		return latitude;
	}
	public double getLongitude(){
		return longitude;
	}
	public double getProductionShare(){
		return productionShare;
	}
	public double getSizeInGuiDisplay(){
		return sizeInGuiDisplay;
	}
	public boolean getExportAllowerFlag(){
		return exportAllowed;
	}
	public int getTimeOfFirstProduction(){
		return timeOfFirstProduction;
	}
	public int getTimeOfFirstProductionSpring(){
		return timeOfFirstProductionSpring;
	}
	public int getStock(){
		return stock;
	}
	public int getProduction(){
		return production;
	}
	public int getProductionWinter(){
		return production_winter;
	}
	public int getProductionSpring(){
		return production_spring;
	}
	public int getTargetProduction(){
		return targetProduction;
	}
	public double getAverageSellingPrice(){
		return averageSellingPrice;
	}
	

}
