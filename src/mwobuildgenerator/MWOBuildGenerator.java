package mwobuildgenerator;
import java.awt.Frame;
import java.awt.Toolkit;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
public interface MWOBuildGenerator{
	static void main(final String[] args){
		final var uiFrame=new Frame("MWO Build Generator");
		uiFrame.setVisible(true);
		final var uiFrameWidth=480;
		final var uiFrameHeight=0;
		uiFrame.setSize(uiFrameWidth,uiFrameHeight);
		final var screenDimensions=Toolkit.getDefaultToolkit().getScreenSize();
		uiFrame.setLocation((int)((screenDimensions.getWidth()-uiFrameWidth)/2),(int)((screenDimensions.getHeight()-uiFrameHeight)/2));
		uiFrame.addWindowListener(new WindowListener(){
			@Override public void windowActivated(final WindowEvent windowEvent){}
			@Override public void windowClosed(final WindowEvent windowEvent){}
			@Override public void windowClosing(final WindowEvent windowEvent){
				uiFrame.dispose();
				System.exit(0);
			}
			@Override public void windowDeactivated(final WindowEvent windowEvent){}
			@Override public void windowDeiconified(final WindowEvent windowEvent){}
			@Override public void windowIconified(final WindowEvent windowEvent){}
			@Override public void windowOpened(final WindowEvent windowEvent){}
		});
		try(final var loggingStream=new PrintStream(new FileOutputStream("MWO Build Generator.log"));){
			System.setErr(loggingStream);
			final var propertiesFetcher=new PropertyFromString("config.properties",new Properties());
			propertiesFetcher.init();
			final var tag=propertiesFetcher.getString("tag");
			final var targetRange=propertiesFetcher.parseInt("targetRange");
			final var targetSpeed=propertiesFetcher.parseFloat("minSpeed");
			final var targetHslGroups=propertiesFetcher.parseInt("hslGroups");
			final var targetEngageDuration=propertiesFetcher.parseFloat("targetEngageDuration")+.5f*(targetHslGroups-1);
			final var targetAmmoLifeSpan=propertiesFetcher.parseInt("ammoLifeSpan");
			final var useJumpCapableOnly=propertiesFetcher.getBit("jumpCapable");
			final var useEcmCapableOnly=propertiesFetcher.getBit("ecmCapable");
			final var targetWeightMin=propertiesFetcher.getInt("minTonnage");
			final var targetWeightMax=propertiesFetcher.getInt("maxTonnage");
			final var engageIntervalFactor=propertiesFetcher.parseFloat("engageIntervalWeightCoeff");
			final var useExactDPE=propertiesFetcher.getBit("useExactDPE");
			final var useEngageInterval=propertiesFetcher.getBit("considerEngageInterval");
			final var baseArmorDecreaseFactor=propertiesFetcher.getFloat("armorTrim");
			final var useEcmWeight=propertiesFetcher.getBit("useECM");
			final var armorPerTonFerroIncreaseFactorIs=propertiesFetcher.parseFloat("ferroCoeffIS");
			final var rac5RampUp=propertiesFetcher.parseFloat("rac5RampUp");
			final var rac5Spin=propertiesFetcher.parseFloat("rac5Spin");
			final var rac2RampUp=propertiesFetcher.parseFloat("rac2RampUp");
			final var rac2Spin=propertiesFetcher.parseFloat("rac2Spin");
			final var capacitySkill=propertiesFetcher.parseFloat("capacitySkill");
			final var dissipationSkill=propertiesFetcher.parseFloat("dissipationSkill");
			final var externalDHSHeatCapacity=propertiesFetcher.parseFloat("edhsCap");
			final var dhsDissipation=propertiesFetcher.parseFloat("dhsDis");
			final var baseMechHeatCapacity=propertiesFetcher.parseInt("baseCapacity");
			final var internalDHSHeatCapacity=propertiesFetcher.parseInt("idhsCap");
			final var jamDurationSkill=propertiesFetcher.parseFloat("jamDurationSkill");
			final var cooldownSkillClan=propertiesFetcher.parseFloat("cooldownSkillClan");
			final var laserDurationSkillClan=propertiesFetcher.parseFloat("durationSkillClan");
			final var weaponHeatSkillClan=propertiesFetcher.parseFloat("weaponHeatSkillClan");
			final var cooldownSkillIs=propertiesFetcher.parseFloat("cooldownSkillIS");
			final var lasurDurationSkillIs=propertiesFetcher.parseFloat("durationSkillIS");
			final var weaponHeatSkillIs=propertiesFetcher.parseFloat("weaponHeatSkillIS");
			final var rangeSkill=propertiesFetcher.parseFloat("rangeSkill");
			final var armorPerTonFerroIncreaseFactorClan=propertiesFetcher.parseFloat("ferroCoeffClan");
			final var armorPerTonLightFerroIncreaseFactorIs=propertiesFetcher.parseFloat("lhtFerroCoeffIS");
			final var speedSkill=propertiesFetcher.parseFloat("speedSkill");
			final var endoStructureFactor=propertiesFetcher.parseFloat("endoCoeff");
			final var standardStructureFactor=propertiesFetcher.parseFloat("stdStrCoeff");
			final var numLoadoutsPerMechConfig=propertiesFetcher.parseInt("numLoadouts");
			final var filePathSeparator=File.separator;
			final var outputDirectoryStructure=tag+" "
				+(useJumpCapableOnly?"jump capable"+filePathSeparator:"")
				+(useEcmCapableOnly?"ecm capable"+filePathSeparator:"")
				+(useEcmWeight?"using ecm"+filePathSeparator:"")
				+(useExactDPE?"using exact shots"+filePathSeparator:"")
				+(targetHslGroups>1?"hsl groups "+targetHslGroups+filePathSeparator:"")
				+(useEngageInterval?"consider engage interval"+filePathSeparator+"interval weight"+engageIntervalFactor+filePathSeparator:"")
				+"min tonnage "
				+targetWeightMin
				+filePathSeparator
				+"max tonnage "
				+targetWeightMax
				+filePathSeparator
				+"armor trim "
				+baseArmorDecreaseFactor
				+filePathSeparator
				+"ammo life span "
				+targetAmmoLifeSpan
				+filePathSeparator
				+"range "
				+targetRange
				+filePathSeparator
				+"speed "
				+targetSpeed
				+filePathSeparator
				+"engage duration "
				+targetEngageDuration;
			final var mechRatingsFileName="mech ratings.txt";
			final BiConsumer<String,Exception> logAndExit=(errorMessage,exception)->{
				loggingStream.println(errorMessage);
				exception.printStackTrace();
				uiFrame.setTitle("MWO Build Generator:error,check log");
				System.exit(-1);
			};
			final var dataFolderPrefix="data/";
			final MeasureRampUpsPerEngage measuredRampUpsPerEngageF=(rampUp,weapon)->(targetEngageDuration>=rampUp?targetEngageDuration<=rampUp
				+weapon.jamDuration?1:targetEngageDuration/(rampUp+weapon.jamDuration):targetEngageDuration/rampUp);
			final FloatOfWeapon measuredPotentialShotsPerEngageF=q->{
				final var cycle=q.cooldown+q.duration;
				return q.specType==4?1+measuredRampUpsPerEngageF.apply(rac2RampUp,q)*(rac2RampUp-rac2Spin)/cycle:q.specType==5?1+measuredRampUpsPerEngageF.apply(
					rac5RampUp,
					q)*(rac5RampUp-rac5Spin)/cycle:1+(targetEngageDuration-q.duration)/cycle;
			};
			final HeatLimitedShots heatLimitedShotsF=(remainingCoolingPerHeat,potentialEngageShots)->Math.max(0,
				Math.min(potentialEngageShots,remainingCoolingPerHeat));
			final var numberPrecisionF=useExactDPE?(FloatFunction)q->(float)(int)q:(FloatFunction)q->q;
			final RemainingCooling remainingCoolingF=(hpdSortedLoadout,heatCapacity,heatDissipation)->{
				final var identity=heatDissipation*targetEngageDuration+heatCapacity;
				return Math.min(heatCapacity,
					hpdSortedLoadout.stream()
						.reduce(identity,
							(a,b)->a-(b.k().heat==0?0
								:b.k().heat*heatLimitedShotsF.apply(numberPrecisionF.apply(a/b.k().heat),
									numberPrecisionF.apply(measuredPotentialShotsPerEngageF.apply(b.k()))*b.v())),
							(a,b)->-identity+a+b));
			};
			final LoadoutWeaponsShots loadoutWeaponsShotsF=(hpdSortedLoadout,capacity,dissipation)->hpdSortedLoadout.map(new Function<QuantityC<Weapon>,
				WeaponShots>(){
				float remainingCooling=dissipation*targetEngageDuration+capacity;
				@Override public WeaponShots apply(final QuantityC<Weapon> entry){
					final var potentialEngageShots=numberPrecisionF.apply(measuredPotentialShotsPerEngageF.apply(entry.k()))*entry.v();
					final var shots=entry.k().heat==0?potentialEngageShots
						:heatLimitedShotsF.apply(numberPrecisionF.apply(remainingCooling/entry.k().heat),potentialEngageShots);
					remainingCooling-=entry.k().heat*shots;
					return new WeaponShots(entry,shots);
				}
			});
			final DPE dpeF=(hpdSortedLoadout,capacity,dissipation)->loadoutWeaponsShotsF.f(hpdSortedLoadout.stream(),capacity,dissipation)
				.reduce(0f,(subDamage,weaponShots)->subDamage+weaponShots.shots*weaponShots.weaponEntry.k().damage,(a,b)->a+b);
			final Collection<Weapon> weaponSet=safeRead(dataFolderPrefix+"weapons.txt",uiFrame).stream().skip(1).map(q->{
				final var weaponTokens=q.split("\t");
				var tokenIncrement=0;
				return new Weapon(weaponTokens[tokenIncrement++],
					weaponTokens[tokenIncrement++].equals("1"),
					WeaponType.valueOf(weaponTokens[tokenIncrement++]),
					weaponTokens[tokenIncrement++].equals("1"),
					Float.parseFloat(weaponTokens[tokenIncrement++]),
					Float.parseFloat(weaponTokens[tokenIncrement++]),
					Float.parseFloat(weaponTokens[tokenIncrement++]),
					Float.parseFloat(weaponTokens[tokenIncrement++]),
					Float.parseFloat(weaponTokens[tokenIncrement++]),
					Float.parseFloat(weaponTokens[tokenIncrement++]),
					Integer.parseInt(weaponTokens[tokenIncrement++]),
					Integer.parseInt(weaponTokens[tokenIncrement++]),
					Integer.parseInt(weaponTokens[tokenIncrement++]),
					Integer.parseInt(weaponTokens[tokenIncrement++]),
					Float.parseFloat(weaponTokens[tokenIncrement++]),
					Integer.parseInt(weaponTokens[tokenIncrement++]),
					Float.parseFloat(weaponTokens[tokenIncrement++]),
					Float.parseFloat(weaponTokens[tokenIncrement++]),
					Integer.parseInt(weaponTokens[tokenIncrement++]),
					Integer.parseInt(weaponTokens[tokenIncrement++]),
					Integer.parseInt(weaponTokens[tokenIncrement++]));
			}).collect(Collectors.toList());
			final FloatOfWeapon weaponAmmoWeightMeasuredF=q->q.weaponType.equals(WeaponType.ENY)?0
				:targetAmmoLifeSpan/((q.ammo+q.xAmmo)/q.salvo*(q.cooldown+q.duration));
			final AmmoWeightHalfCardinalRounded ammoWeightHalfCardinalRoundedF=measuredAmmoWeight->measuredAmmoWeight==0?0
				:Math.max(.5f,Math.round(measuredAmmoWeight*2)/2f);
			final MslAmmoWeight mslAmmoWeightF=(loadout,filter)->ammoWeightHalfCardinalRoundedF.apply(loadout.stream()
				.filter(q->filter.test(q.k()))
				.map(q->weaponAmmoWeightMeasuredF.apply(q.k())*q.v())
				.reduce(0f,(a,b)->a+b));
			final Predicate<Weapon> srmFilter=q->q.specType==3;
			final Predicate<Weapon> mrmFilter=q->q.specType==2;
			final LoadoutAmmoWeightHalfCardinalRoundedF loadoutAmmoWeightHalfCardinalRoundedF=q->(q.stream()
				.filter(w->(!srmFilter.test(w.k())&&!mrmFilter.test(w.k())))
				.map(w->ammoWeightHalfCardinalRoundedF.apply(w.v()*weaponAmmoWeightMeasuredF.apply(w.k())))
				.reduce(0f,(a,b)->a+b)+mslAmmoWeightF.apply(q,srmFilter)
				+mslAmmoWeightF.apply(q,mrmFilter));
			new File(outputDirectoryStructure).mkdirs();
			final var mechLoadoutsFileName="mech loadouts.txt";
			FileOutputStream mechLoadoutsFileStream;
			try{
				mechLoadoutsFileStream=new FileOutputStream(outputDirectoryStructure+filePathSeparator+mechLoadoutsFileName);
			}catch(final FileNotFoundException exception){
				logAndExit.accept("failed to generate "+outputDirectoryStructure+filePathSeparator+mechLoadoutsFileName,exception);
				throw new Exception();
			}
			FileOutputStream mechRatingsFileStream;
			try{
				mechRatingsFileStream=new FileOutputStream(outputDirectoryStructure+filePathSeparator+mechRatingsFileName);
			}catch(final FileNotFoundException exception){
				logAndExit.accept("failed to generate "+outputDirectoryStructure+filePathSeparator+mechRatingsFileName,exception);
				throw new Exception();
			}
			final Map<String,Integer> chassisToMascClassMap=mapFromFile(1,1,q->q,Integer::valueOf,safeRead(dataFolderPrefix+"chassis masc class.txt",uiFrame));
			final Map<Integer,Float> mascClassToSpeedMapIS=mapFromFile(1,
				1,
				Integer::valueOf,
				Float::valueOf,
				safeRead(dataFolderPrefix+"masc class speed IS.txt",uiFrame));
			final Map<Integer,Float> mascClassToSpeedMapClan=mapFromFile(1,
				1,
				Integer::valueOf,
				Float::valueOf,
				safeRead(dataFolderPrefix+"masc class speed clan.txt",uiFrame));
			final var engineRatingToWeightLines=safeRead(dataFolderPrefix+"engine rating weight.txt",uiFrame);
			final Map<Integer,Integer> mechWeightToArmorMap=mapFromFile(1,
				1,
				Integer::valueOf,
				Integer::valueOf,
				safeRead(dataFolderPrefix+"mech tonnage armor.txt",uiFrame));
			final Map<Integer,Float> mechWeightToJumpJetDistanceMap=mapFromFile(1,
				1,
				Integer::valueOf,
				Float::valueOf,
				safeRead(dataFolderPrefix+"mech tonnage jump jet distance.txt",uiFrame));
			final Map<Integer,Float> jumpJetClassToWeightMap=mapFromFile(1,
				1,
				Integer::valueOf,
				Float::valueOf,
				safeRead(dataFolderPrefix+"jump jet class weight.txt",uiFrame));
			final var maxEngineHeatSinks=10;
			final EngineHeatSinkSocketsF engineHeatSinkSocketsF=requiredEngineForTargetSpeed->((int)Math.max(Math.floor(requiredEngineForTargetSpeed*.04f)
				-maxEngineHeatSinks,0));
			final IntOfLoadout weaponSlotsF=q->q.stream().reduce(0,(subTotalWeaponSlots,entry)->subTotalWeaponSlots+entry.k().slots*entry.v(),(a,b)->a+b);
			final IntOfLoadout weaponAndAmmoSlotsF=q->((int)(weaponSlotsF.apply(q)+(q.stream()
				.filter(w->(!srmFilter.test(w.k())&&!mrmFilter.test(w.k())))
				.map(w->Math.ceil(ammoWeightHalfCardinalRoundedF.apply(w.v()*weaponAmmoWeightMeasuredF.apply(w.k()))))
				.reduce((a,b)->a+b)
				.orElseGet(()->0d)+(int)Math.ceil(mslAmmoWeightF.apply(q,srmFilter))
				+(int)Math.ceil(mslAmmoWeightF.apply(q,mrmFilter)))));
			final var startTime=System.currentTimeMillis();
			final var engineSpeedCoeff=16.2f;
			final var averageMascSpeedBonusDivisor=3;
			final var filteredMechsList=safeRead(dataFolderPrefix+"mechs.txt",uiFrame).stream().skip(1).map(q->{
				final var mechTokens=q.split("\t");
				var tokenIncrement=0;
				return new Mech(mechTokens[tokenIncrement++],
					mechTokens[tokenIncrement++],
					mechTokens[tokenIncrement++].equals("1"),
					mechTokens[tokenIncrement++].equals("1"),
					Integer.parseInt(mechTokens[tokenIncrement++]),
					Short.parseShort(mechTokens[tokenIncrement++]),
					mechTokens[tokenIncrement++].equals("1"),
					mechTokens[tokenIncrement++].equals("1"),
					mechTokens[tokenIncrement++].equals("1"),
					mechTokens[tokenIncrement++].equals("1"),
					Integer.parseInt(mechTokens[tokenIncrement++]),
					Integer.parseInt(mechTokens[tokenIncrement++]),
					Integer.parseInt(mechTokens[tokenIncrement++]),
					Integer.parseInt(mechTokens[tokenIncrement++]),
					Integer.parseInt(mechTokens[tokenIncrement++]),
					Integer.parseInt(mechTokens[tokenIncrement++]),
					Integer.parseInt(mechTokens[tokenIncrement++]),
					Integer.parseInt(mechTokens[tokenIncrement++]),
					Integer.parseInt(mechTokens[tokenIncrement++]),
					Integer.parseInt(mechTokens[tokenIncrement++]),
					Integer.parseInt(mechTokens[tokenIncrement++]),
					Integer.parseInt(mechTokens[tokenIncrement++]),
					Integer.parseInt(mechTokens[tokenIncrement++]),
					Integer.parseInt(mechTokens[tokenIncrement++]),
					Integer.parseInt(mechTokens[tokenIncrement++]),
					Integer.parseInt(mechTokens[tokenIncrement++]),
					Integer.parseInt(mechTokens[tokenIncrement++]),
					Integer.parseInt(mechTokens[tokenIncrement++]),
					mechTokens[tokenIncrement++].equals("1"),
					Float.parseFloat(mechTokens[tokenIncrement++]),
					Float.parseFloat(mechTokens[tokenIncrement++]),
					StrType.valueOf(mechTokens[tokenIncrement++]),
					ArmorType.valueOf(mechTokens[tokenIncrement++]),
					Short.parseShort(mechTokens[tokenIncrement++]),
					Short.parseShort(mechTokens[tokenIncrement++]),
					Float.parseFloat(mechTokens[tokenIncrement++]),
					Float.parseFloat(mechTokens[tokenIncrement++]),
					Float.parseFloat(mechTokens[tokenIncrement++]),
					Float.parseFloat(mechTokens[tokenIncrement++]),
					Float.parseFloat(mechTokens[tokenIncrement++]),
					Float.parseFloat(mechTokens[tokenIncrement++]),
					Float.parseFloat(mechTokens[tokenIncrement++]),
					Float.parseFloat(mechTokens[tokenIncrement++]),
					Float.parseFloat(mechTokens[tokenIncrement++]),
					Float.parseFloat(mechTokens[tokenIncrement++]),
					Float.parseFloat(mechTokens[tokenIncrement++]),
					Float.parseFloat(mechTokens[tokenIncrement++]),
					Float.parseFloat(mechTokens[tokenIncrement++]),
					Float.parseFloat(mechTokens[tokenIncrement++]),
					Float.parseFloat(mechTokens[tokenIncrement++]),
					Float.parseFloat(mechTokens[tokenIncrement++]),
					Float.parseFloat(mechTokens[tokenIncrement++]),
					Float.parseFloat(mechTokens[tokenIncrement++]),
					Float.parseFloat(mechTokens[tokenIncrement++]),
					Float.parseFloat(mechTokens[tokenIncrement++]),
					Float.parseFloat(mechTokens[tokenIncrement++]),
					Float.parseFloat(mechTokens[tokenIncrement++]),
					Float.parseFloat(mechTokens[tokenIncrement++]),
					Float.parseFloat(mechTokens[tokenIncrement++]),
					Float.parseFloat(mechTokens[tokenIncrement++]),
					Float.parseFloat(mechTokens[tokenIncrement++]),
					Float.parseFloat(mechTokens[tokenIncrement++]),
					Float.parseFloat(mechTokens[tokenIncrement++]),
					Float.parseFloat(mechTokens[tokenIncrement++]),
					Float.parseFloat(mechTokens[tokenIncrement++]),
					Float.parseFloat(mechTokens[tokenIncrement++]),
					Float.parseFloat(mechTokens[tokenIncrement++]),
					Float.parseFloat(mechTokens[tokenIncrement++]),
					Float.parseFloat(mechTokens[tokenIncrement++]),
					Float.parseFloat(mechTokens[tokenIncrement++]),
					Float.parseFloat(mechTokens[tokenIncrement++]),
					Float.parseFloat(mechTokens[tokenIncrement++]),
					Float.parseFloat(mechTokens[tokenIncrement++]),
					Float.parseFloat(mechTokens[tokenIncrement++]),
					Float.parseFloat(mechTokens[tokenIncrement++]),
					Float.parseFloat(mechTokens[tokenIncrement++]),
					Float.parseFloat(mechTokens[tokenIncrement++]),
					Float.parseFloat(mechTokens[tokenIncrement++]),
					Float.parseFloat(mechTokens[tokenIncrement++]),
					Float.parseFloat(mechTokens[tokenIncrement++]),
					Float.parseFloat(mechTokens[tokenIncrement++]),
					Float.parseFloat(mechTokens[tokenIncrement++]),
					Float.parseFloat(mechTokens[tokenIncrement++]),
					Float.parseFloat(mechTokens[tokenIncrement++]),
					Float.parseFloat(mechTokens[tokenIncrement++]),
					Float.parseFloat(mechTokens[tokenIncrement++]),
					Float.parseFloat(mechTokens[tokenIncrement++]),
					Float.parseFloat(mechTokens[tokenIncrement++]),
					Float.parseFloat(mechTokens[tokenIncrement++]),
					Float.parseFloat(mechTokens[tokenIncrement++]),
					Float.parseFloat(mechTokens[tokenIncrement++]),
					Float.parseFloat(mechTokens[tokenIncrement++]),
					Float.parseFloat(mechTokens[tokenIncrement++]),
					Float.parseFloat(mechTokens[tokenIncrement++]),
					Float.parseFloat(mechTokens[tokenIncrement++]),
					Float.parseFloat(mechTokens[tokenIncrement++]),
					Float.parseFloat(mechTokens[tokenIncrement++]),
					Float.parseFloat(mechTokens[tokenIncrement++]),
					Float.parseFloat(mechTokens[tokenIncrement++]),
					Float.parseFloat(mechTokens[tokenIncrement++]),
					Float.parseFloat(mechTokens[tokenIncrement++]),
					Float.parseFloat(mechTokens[tokenIncrement++]),
					Float.parseFloat(mechTokens[tokenIncrement++]),
					Float.parseFloat(mechTokens[tokenIncrement++]),
					Float.parseFloat(mechTokens[tokenIncrement++]),
					Float.parseFloat(mechTokens[tokenIncrement++]),
					Float.parseFloat(mechTokens[tokenIncrement++]),
					Float.parseFloat(mechTokens[tokenIncrement++]),
					Integer.parseInt(mechTokens[tokenIncrement++]),
					Integer.parseInt(mechTokens[tokenIncrement++]),
					Integer.parseInt(mechTokens[tokenIncrement++]),
					Integer.parseInt(mechTokens[tokenIncrement++]),
					Integer.parseInt(mechTokens[tokenIncrement++]),
					Integer.parseInt(mechTokens[tokenIncrement++]),
					Integer.parseInt(mechTokens[tokenIncrement++]),
					Integer.parseInt(mechTokens[tokenIncrement++]),
					Integer.parseInt(mechTokens[tokenIncrement++]),
					Float.parseFloat(mechTokens[tokenIncrement++]),
					Float.parseFloat(mechTokens[tokenIncrement++]),
					Float.parseFloat(mechTokens[tokenIncrement++]),
					Float.parseFloat(mechTokens[tokenIncrement++]));
			}).filter(q->{
				final var mascClassAndWeightAndSlots=q.masc?chassisToMascClassMap.entrySet()
					.stream()
					.filter(w->w.getKey().equalsIgnoreCase(q.chassis))
					.findAny()
					.get()
					.getValue():0;
				return (useJumpCapableOnly?q.jumpDist>0:true)&&q.mechWeight>=targetWeightMin
					&&q.mechWeight<=targetWeightMax
					&&(useEcmCapableOnly?q.ecm:true)
					&&q.maxEng*engineSpeedCoeff
						/q.mechWeight
						*(1+(q.masc?q.clan?mascClassToSpeedMapClan.get(mascClassAndWeightAndSlots):mascClassToSpeedMapIS.get(mascClassAndWeightAndSlots):0)
							/averageMascSpeedBonusDivisor)
						*(1+speedSkill)>=targetSpeed;
			}).collect(Collectors.toList());
			try(final var mechLoadoutsPrintStream=new PrintStream(mechLoadoutsFileStream);
				final var mechRatingsPrintStream=new PrintStream(mechRatingsFileStream);){
				filteredMechsList.stream().parallel().<Op>map(new Function<Mech,Op>(){
					int currentMechIndex;
					@Override public Op apply(final Mech q){
						uiFrame.setTitle("MWO Build Generator:"+Math.floor((float)currentMechIndex/filteredMechsList.size()*10000)/100+"%"+q.chassis+" "+q.variant);
						final var mascClassAndWeightAndSlots=q.masc?chassisToMascClassMap.entrySet()
							.stream()
							.filter(w->w.getKey().equalsIgnoreCase(q.chassis))
							.findAny()
							.get()
							.getValue():0;
						final var speedIncMascCoeff=q.masc?q.clan?mascClassToSpeedMapClan.get(mascClassAndWeightAndSlots)
							:mascClassToSpeedMapIS.get(mascClassAndWeightAndSlots):0;
						final var requiredEngineForTargetSpeed=q.omni?q.maxEng:q.maxEng<=250?q.maxEng
							:(int)Math.max(250,
								Math.min(q.maxEng,
									Math.ceil(targetSpeed*(1-speedIncMascCoeff/averageMascSpeedBonusDivisor)*(1-speedSkill)*q.mechWeight/engineSpeedCoeff/10)*10));
						final var socketedHeatSinks=q.omni?engineHeatSinkSocketsF.apply(requiredEngineForTargetSpeed):0;
						final var jumpJets=q.omni?q.jumpDist>0?Math.round(q.jumpDist/mechWeightToJumpJetDistanceMap.entrySet()
							.stream()
							.filter(w->w.getKey()==q.mechWeight)
							.findAny()
							.get()
							.getValue()):0:0;
						final var fixedJumpJetsWeight=q.omni?jumpJets*jumpJetClassToWeightMap.entrySet()
							.stream()
							.filter(w->w.getKey()==(q.mechWeight<40?5:q.mechWeight<60?4:q.mechWeight<80?3:q.mechWeight<=85?2:1))
							.findAny()
							.get()
							.getValue():0;
						final var engineHeatSinkSockets=engineHeatSinkSocketsF.apply(requiredEngineForTargetSpeed);
						final AddedHeatsinks addedHeatSinksF=(loadout,adjAvailSlots,adjAvailWeight)->Math.min((int)Math.floor(adjAvailWeight-(loadout.stream()
							.reduce(0f,(subTotalWeaponWeight,weaponQuantity)->subTotalWeaponWeight+weaponQuantity.k().weight*weaponQuantity.v(),(a,b)->a+b)
							+loadoutAmmoWeightHalfCardinalRoundedF.apply(loadout))),
							(adjAvailSlots-weaponAndAmmoSlotsF.apply(loadout))/(q.clan?2:3)+(q.omni?0:engineHeatSinkSockets));
						final var armorPerTon=32;
						final var baseArmorMinusHead=mechWeightToArmorMap.entrySet().stream().filter(w->w.getKey()==q.mechWeight).findAny().get().getValue()-18;
						final var baseArmorTrimmed=baseArmorMinusHead*(1-baseArmorDecreaseFactor)+8;
						final var engineDoubleHeatSinks=(int)Math.min(Math.floor(requiredEngineForTargetSpeed*.04f),maxEngineHeatSinks);
						final var structureWeight=q.strType.equals(StrType.STD)?q.mechWeight*standardStructureFactor:q.mechWeight*endoStructureFactor;
						final var engineHeatSinkDeficit=maxEngineHeatSinks-engineDoubleHeatSinks;
						final var takingECM=useEcmWeight&&q.ecm;
						final var ecmWeight=takingECM?q.clan?1:1.5f:0;
						final var chosenStructureType=q.strType.equals(StrType.STD)?StrType.STD:StrType.ENDO;
						final var chosenStructureSlots=q.clan?chosenStructureType.equals(StrType.ENDO)?7:0:chosenStructureType.equals(StrType.ENDO)?14:0;
						final var ecmSlots=q.clan?takingECM?1:0:takingECM?2:0;
						final var requiredExternalDoubleHeatSinks=socketedHeatSinks+engineHeatSinkDeficit;
						final var SkilledQuirkedDoubleHeatSinkDissipation=dhsDissipation*(1+(dissipationSkill+q.hDis));
						final FloatOfInt heatDissipationF=addedHeatSinks->(engineDoubleHeatSinks+requiredExternalDoubleHeatSinks+addedHeatSinks)
							*SkilledQuirkedDoubleHeatSinkDissipation;
						final FloatOfInt heatCapacityF=addedHeatSinks->baseMechHeatCapacity+engineDoubleHeatSinks*internalDHSHeatCapacity*(1+capacitySkill)
							+(requiredExternalDoubleHeatSinks+addedHeatSinks)*externalDHSHeatCapacity*(1+capacitySkill);
						final var lifeSupportHeadSlots=2;
						final var sensorsHeadSlots=2;
						final var cockpitHeadSlots=1;
						final var engineCenterTorsoSlots=6;
						final var gyroCenterTorsoSlots=4;
						final var hipUpperLowerFootLegSlots=4;
						final var shoulderUpperArmSlots=2;
						final Collection<Weapon> skilledQuirkedWeaponsSet=weaponSet.stream()
							.filter(w->w.clan==q.clan)
							.map((Function<? super Weapon,? extends Weapon>)w->{
								final var specQuirks=switch(w.name){
									case "LB2-X"->new Q(q.lbxCd,0,0,0,0,0,0);
									case "LB5-X"->new Q(q.lbxCd,0,0,0,0,0,0);
									case "C-AC/2"->new Q(q.ac2Cd,0,0,0,0,0,0);
									case "C-LB2-X"->new Q(q.lbxCd,0,0,0,0,0,0);
									case "C-UAC/2"->new Q(q.uacCd,0,q.uac2H,q.uacCh,0,0,q.uacRng);
									case "ERPPC"->new Q(q.erppcCd+q.ppcCd,0,q.erppcH+q.ppcH,0,0,q.erppcHsl+q.ppcFamHsl+q.ppcHsl,q.ppcRng);
									case "C-ERPPC"->new Q(q.erppcCd+q.ppcCd,0,q.erppcH+q.ppcH,0,0,q.erppcHsl+q.ppcFamHsl+q.ppcHsl,q.ppcRng);
									case "LB10-X"->new Q(q.lbxCd+q.lbx10Cd,0,0,0,0,0,0);
									case "L-GAUSS"->new Q(0,0,0,0,0,0,0);
									case "C-ERLL"->new Q(q.erlCd+q.erllCd+q.llCd,q.erlDur,q.erlH+q.erllH+q.llH,0,0,q.erllHsl,q.erlRng+q.llRng);
									case "AC/2"->new Q(q.ac2Cd,0,0,0,0,0,0);
									case "C-AC/5"->new Q(q.ac5Cd,0,0,0,0,0,0);
									case "C-LB5-X"->new Q(q.lbxCd,0,0,0,0,0,0);
									case "UAC/2"->new Q(q.uacCd,0,q.uac2H,q.uacCh,0,0,q.uacRng);
									case "ERLL"->new Q(q.erlCd+q.erllCd+q.llCd,q.erlDur,q.erlH+q.erllH+q.llH,0,0,q.erllHsl,q.erlRng+q.llRng);
									case "GAUSS"->new Q(q.gausCd,0,0,0,0,0,0);
									case "C-GAUSS"->new Q(q.gausCd,0,0,0,0,0,0);
									case "C-UAC/5"->new Q(q.uac5Cd+q.uacCd,0,0,q.uac5Ch+q.uacCh,0,0,q.uacRng);
									case "AC/5"->new Q(q.ac5Cd,0,0,0,0,0,0);
									case "UAC/5"->new Q(q.uac5Cd+q.uacCd,0,0,q.uac5Ch+q.uacCh,0,0,q.uacRng);
									case "PPC"->new Q(q.ppcCd,0,q.ppcH,0,0,q.ppcFamHsl+q.ppcHsl,q.ppcRng);
									case "L-PPC"->new Q(q.ppcCd,0,q.ppcH,0,0,q.ppcFamHsl+q.lppcHsl+q.ppcHsl,q.ppcRng);
									case "H-PPC"->new Q(q.ppcCd,0,q.ppcH,0,0,q.ppcFamHsl+q.ppcHsl,q.ppcRng);
									case "RAC/2"->new Q(0,0,0,0,q.racJamRampDownDur,0,0);
									case "C-AC/10"->new Q(q.ac10Cd,0,0,0,0,0,q.ac10Rng);
									case "C-UAC/10"->new Q(q.uacCd,0,0,q.uacCh,0,0,q.uacRng);
									case "C-LB10-X"->new Q(q.lbxCd+q.lbx10Cd,0,0,0,0,0,0);
									case "LB20-X"->new Q(q.lbxCd,0,0,0,0,0,0);
									case "RAC/5"->new Q(0,0,0,0,q.racJamRampDownDur,0,0);
									case "UAC/10"->new Q(q.uacCd,0,0,q.uacCh,0,0,q.uacRng);
									case "AC/10"->new Q(q.ac10Cd,0,0,0,0,0,q.ac10Rng);
									case "LL"->new Q(q.llCd,q.stdLsrDur,q.llH+q.stdLsrH,0,0,0,q.llRng+q.stdLsrRng);
									case "C-HLL"->new Q(q.hlCd,q.hlDur+q.llCd,q.hlH+q.llH,0,0,q.hlHsl,q.hlRng+q.llRng);
									case "H-GAUSS"->new Q(0,0,0,0,0,0,0);
									case "C-LPL"->new Q(q.llCd+q.lplCd+q.plCd,q.plDur,q.llH+q.lplH+q.plH,0,0,q.lplHsl,q.llRng+q.lplRng+q.plRng);
									case "C-ERML"->new Q(q.erlCd+q.mlCd,q.erlDur+q.mlDur,q.erlH+q.ermlH+q.mlH,0,0,q.mlHsl,q.erlRng+q.mlRng);
									case "LPL"->new Q(q.llCd+q.lplCd+q.plCd,q.plDur,q.llH+q.lplH+q.plH,0,0,q.lplHsl,q.llRng+q.lplRng+q.plRng);
									case "C-AC/20"->new Q(q.ac20Cd,0,q.ac20H,0,0,0,0);
									case "C-UAC/20"->new Q(q.uac20Cd+q.uacCd,0,0,q.uac20Ch+q.uacCh,0,q.uac20Hsl,q.uacRng);
									case "C-LB20-X"->new Q(q.lbxCd,0,0,0,0,0,0);
									case "ERML"->new Q(q.erlCd+q.mlCd,q.erlDur+q.mlDur,q.erlH+q.ermlH+q.mlH,0,0,q.mlHsl,q.erlRng+q.mlRng);
									case "SN-PPC"->new Q(q.ppcCd,0,q.ppcH,0,0,q.ppcFamHsl+q.ppcHsl,q.ppcRng);
									case "UAC/20"->new Q(q.uac20Cd+q.uacCd,0,0,q.uac20Ch+q.uacCh,0,q.uac20Hsl,q.uacRng);
									case "AC/20"->new Q(q.ac20Cd,0,q.ac20H,0,0,0,0);
									case "ML"->new Q(q.mlCd,q.mlDur+q.stdLsrDur,q.mlH+q.stdLsrH,0,0,q.mlHsl,q.mlRng+q.stdLsrRng);
									case "C-HML"->new Q(q.hlCd+q.mlCd,q.hlDur+q.mlDur,q.hlH+q.hmlH+q.mlH,0,0,q.hlHsl+q.mlHsl,q.hlRng+q.mlRng);
									case "LMG"->new Q(q.mgROF,0,0,0,0,0,q.mgRng);
									case "C-LMG"->new Q(q.mgROF,0,0,0,0,0,q.mgRng);
									case "C-MPL"->new Q(q.mplCd+q.plCd,q.plDur,q.mplH+q.plH,0,0,0,q.mplRng+q.plRng);
									case "MPL"->new Q(q.mplCd+q.plCd,q.plDur,q.mplH+q.plH,0,0,0,q.mplRng+q.plRng);
									case "ERSL"->new Q(q.erlCd,q.erlDur,q.erlH,0,0,0,q.erlRng);
									case "C-ERSL"->new Q(q.erlCd,q.erlDur,q.erlH,0,0,0,q.erlRng);
									case "SL"->new Q(0,q.stdLsrDur,q.stdLsrH,0,0,0,q.stdLsrRng);
									case "C-ERMCL"->new Q(q.erlCd,q.erlDur,q.erlH,0,0,0,q.erlRng);
									case "C-SPL"->new Q(q.plCd,q.plDur,q.plH,0,0,0,q.plRng);
									case "MG"->new Q(q.mgROF,0,0,0,0,0,q.mgRng);
									case "C-MG"->new Q(0,0,q.mgROF,0,0,0,q.mgRng);
									case "C-HSL"->new Q(q.hlCd,q.hlDur,q.hlH,0,0,q.hlHsl,q.hlRng);
									case "SPL"->new Q(q.plCd,q.plDur,q.plH,0,0,0,q.plRng);
									case "HMG"->new Q(q.mgROF,0,0,0,0,0,q.mgRng);
									case "C-HMG"->new Q(0,0,q.mgROF,0,0,0,q.mgRng);
									case "C-MCPL"->new Q(q.plCd,q.plDur,q.plH,0,0,0,q.plRng);
									case "C-SRM 2"->new Q(0,0,q.srmH+q.srm2H,0,0,0,0);
									case "C-SRM 4"->new Q(q.srm4Cd,0,q.srmH,0,0,0,0);
									case "C-SRM 6"->new Q(0,0,q.srmH,0,0,0,0);
									case "SRM 2"->new Q(0,0,q.srmH+q.srm2H,0,0,0,0);
									case "SRM 4"->new Q(q.srm4Cd,0,q.srmH,0,0,0,0);
									case "SRM 6"->new Q(0,0,q.srmH,0,0,0,0);
									case "MRM 10"->new Q(0,0,0,0,0,0,0);
									case "MRM 20"->new Q(0,0,0,0,0,0,0);
									case "MRM 30"->new Q(0,0,0,0,0,0,0);
									case "MRM 40"->new Q(0,0,0,0,0,0,0);
									default->throw new RuntimeException("failed to match weapon name:"+w.name);
								};
								final float typedCooldownQuirk;
								final float typedHeatQuirk;
								final float typedRangeQuirk;
								final float newDuration;
								float factionHeatSkill;
								factionHeatSkill=w.clan?weaponHeatSkillClan:weaponHeatSkillIs;
								if(w.weaponType.equals(WeaponType.BTC)){
									newDuration=w.duration;
									typedCooldownQuirk=q.btcCd;
									typedHeatQuirk=q.btcH;
									typedRangeQuirk=q.btcRng;
								}else if(w.weaponType.equals(WeaponType.MSL)){
									newDuration=w.duration;
									typedCooldownQuirk=q.mslCd;
									typedHeatQuirk=q.mslH;
									typedRangeQuirk=q.mslRng;
								}else{
									float factionDurationSkill;
									factionDurationSkill=w.clan?laserDurationSkillClan:lasurDurationSkillIs;
									newDuration=w.duration*(1-(factionDurationSkill+q.lsrDur+specQuirks.dur));
									if(w.specType==1){
										typedCooldownQuirk=q.enyCd;
										typedHeatQuirk=q.enyH;
										typedRangeQuirk=q.enyRng;
									}else{
										typedCooldownQuirk=q.enyCd+q.lsrCd;
										typedHeatQuirk=q.enyH+q.lsrH;
										typedRangeQuirk=q.enyRng+q.lsrRng;
									}
								}
								final var typedRangeSkillQuirk=specQuirks.rng+rangeSkill+q.rng+typedRangeQuirk;
								final var newRange=w.range*(1+typedRangeSkillQuirk);
								final var newMaxRange=w.maxRange*(1+typedRangeSkillQuirk);
								final var newJamChance=w.jamChance*(1-specQuirks.jc);
								final var newJamDuration=w.jamDuration*(1-(jamDurationSkill+specQuirks.jd));
								return new Weapon(w.name,
									w.clan,
									w.weaponType,
									w.continuous,
									newMaxRange<=targetRange?0:newRange<targetRange&&newMaxRange-newRange!=0?Math.min(w.damage,
										Math.max(0,w.damage/((newMaxRange-newRange)/(newMaxRange-targetRange)))):w.damage,
									w.continuous?newJamDuration*newJamChance+w.cooldown*(1-specQuirks.cd)
										:newJamDuration*newJamChance+w.cooldown*(1-((w.clan?cooldownSkillClan:cooldownSkillIs)+(specQuirks.cd+q.cd+typedCooldownQuirk))),
									newJamChance,
									newJamDuration,
									newDuration,
									w.weight,
									w.slots,
									w.ammo,
									w.xAmmo,
									w.salvo,
									w.heat*(1-(factionHeatSkill+(specQuirks.h+q.h+typedHeatQuirk))),
									w.minRange,
									newRange,
									newMaxRange,
									w.heatScaleLimit==0?0:w.heatScaleLimit+specQuirks.hsl,
									w.hslGroup,
									w.specType);
							})
							.collect(Collectors.toList());
						final List<Op> mechEngineOutputs=List.of(EngineType.STD,EngineType.LHT,EngineType.XL)
							.stream()
							.filter(w->((!q.omni||w.equals(EngineType.XL))&&(!q.clan||!w.equals(EngineType.LHT))))
							.map(w->{
								final var reqEngForTargetSpdWeight=mapFromFile(q.omni?2:w.equals(EngineType.STD)?1:w.equals(EngineType.XL)?2:q.clan?2:3,
									1,
									Integer::valueOf,
									Float::valueOf,
									engineRatingToWeightLines).entrySet().stream().filter(e->e.getKey()==requiredEngineForTargetSpeed).findAny().get().getValue();
								final var chosenEngineSideTorsoSlots=q.clan?w.equals(EngineType.STD)?0:2:w.equals(EngineType.STD)?0:w.equals(EngineType.LHT)?2:3;
								final var availLeftArmSlots=12-shoulderUpperArmSlots
									-(q.armYaw&&!q.omni?1+(q.leftHand?1:0):q.amrType.equals(ArmorType.FER)&&q.strType.equals(StrType.ENDO)?1:0);
								final var availRightArmSlots=12-shoulderUpperArmSlots
									-(q.armYaw&&!q.omni?1+(q.rightHand?1:0):q.amrType.equals(ArmorType.FER)&&q.strType.equals(StrType.ENDO)?1:0);
								final HardpointSlots hardpointSlotsF=(raHp,rtHp,ltHp,laHp,ctHp,hHp)->{
									final var availSideTorsoSlots=12-chosenEngineSideTorsoSlots;
									return (raHp>0?(int)availRightArmSlots:0)+(rtHp>0?availSideTorsoSlots:0)
										+(ltHp>0?availSideTorsoSlots:0)
										+(laHp>0?(int)availLeftArmSlots:0)
										+(ctHp>0?(int)(12-engineCenterTorsoSlots-gyroCenterTorsoSlots):0)
										+(hHp>0?(int)(6-lifeSupportHeadSlots-sensorsHeadSlots-cockpitHeadSlots):0);
								};
								final var availBtcSlots=hardpointSlotsF.f(q.raB,q.rtB,q.ltB,q.laB,q.ctB,q.hB);
								final var availEnySlots=hardpointSlotsF.f(q.raE,q.rtE,q.ltE,q.laE,q.ctE,q.hE);
								final var availMslSlots=hardpointSlotsF.f(q.raM,q.rtM,q.ltM,q.laM,q.ctM,q.hM);
								final List<Op> mechArmorOutputs=List.of(ArmorType.STD,ArmorType.LHT,ArmorType.FER)
									.stream()
									.filter(e->((!q.clan||!e.equals(ArmorType.LHT))&&(q.amrType.equals(ArmorType.ANY)||e.equals(q.amrType))))
									.map(e->{
										final float armWeight;
										final float armorWeight;
										if(e.equals(ArmorType.STD)){
											armorWeight=(baseArmorTrimmed-q.bnsAmr)/armorPerTon;
											armWeight=baseArmorMinusHead*.1f/armorPerTon;
										}else{
											final var armorIncCoeff=q.clan?armorPerTonFerroIncreaseFactorClan:e.equals(ArmorType.FER)?armorPerTonFerroIncreaseFactorIs
												:armorPerTonLightFerroIncreaseFactorIs;
											armorWeight=(baseArmorTrimmed-q.bnsAmr)/(armorPerTon*(1+armorIncCoeff));
											armWeight=baseArmorMinusHead*.1f/(armorPerTon*(1+armorIncCoeff));
										}
										final var availableMechWeight=q.mechWeight-fixedJumpJetsWeight
											-mascClassAndWeightAndSlots
											-structureWeight
											-armorWeight
											-reqEngForTargetSpdWeight
											-engineHeatSinkDeficit
											-socketedHeatSinks
											-ecmWeight;
										final var availMechSlots=78-chosenStructureSlots
											-(q.clan?e.equals(ArmorType.FER)||e.equals(ArmorType.LHT)?7:0:e.equals(ArmorType.FER)?14:e.equals(ArmorType.LHT)?7:0)
											-lifeSupportHeadSlots
											-sensorsHeadSlots
											-cockpitHeadSlots
											-engineCenterTorsoSlots
											-gyroCenterTorsoSlots
											-hipUpperLowerFootLegSlots*2
											-shoulderUpperArmSlots*2
											-(q.armYaw&&!q.omni?2+(q.leftHand?1:0)+(q.rightHand?1:0):0)
											-chosenEngineSideTorsoSlots*2
											-ecmSlots
											-mascClassAndWeightAndSlots
											-jumpJets;
										final SingleArmStrip canStripArmOfHardPointTypeF=(availArmSlots,
											availHardpointSlotsOfType,
											hardpointsOfType,
											armHardpointsOfType,
											weaponSlotsOfType,
											usedHardpointsOfType,
											availMechSlots11,
											loadoutSlots1)->hardpointsOfType-armHardpointsOfType>=usedHardpointsOfType&&availMechSlots11-availArmSlots>=loadoutSlots1
												&&availHardpointSlotsOfType-availArmSlots>=weaponSlotsOfType;
										final var enyHP=q.raE+q.rtE+q.ltE+q.laE+q.ctE+q.hE;
										final var btcHP=q.raB+q.rtB+q.ltB+q.laB+q.ctB+q.hB;
										final var mslHP=q.raM+q.rtM+q.ltM+q.laM+q.ctM+q.hM;
										final EngageInterval engageIntervalF=(heatCap,remCool,heatDis)->Math.max((heatCap-remCool)/heatDis,0);
										final DPEPerWeightedResetTime dpePerWeightedResetTimeF=(loadout4,cap,dis,engInter)->(dpeF.f(loadout4,cap,dis)-engInter
											*engageIntervalFactor);
										final var filteredSkilledQuirkedWeaponsSet=skilledQuirkedWeaponsSet.stream()
											.filter(r->r.maxRange>=targetRange&&r.minRange<=targetRange)
											.collect(Collectors.toList());
										final CanStripArm canStripBothArmsF=(usedEnyHP1,usedBtcHP1,usedMslHP1,usedEnySlots1,usedBtcSlots1,usedMslSlots1,weaponAndAmmoSlots1)->{
											final DoubleArmStrip canStripBothArmsOfHardPointTypeF=(armHPA,
												armHPB,
												availHPSlots4,
												hp4,
												weaponSlotsOfType,
												usedHP4)->canStripArmOfHardPointTypeF.f(availLeftArmSlots,
													availHPSlots4,
													hp4,
													armHPB,
													weaponSlotsOfType,
													usedHP4,
													availMechSlots,
													weaponAndAmmoSlots1)&&canStripArmOfHardPointTypeF.f(availRightArmSlots,
														availHPSlots4-availRightArmSlots,
														hp4-armHPB,
														armHPA,
														weaponSlotsOfType,
														usedHP4,
														availMechSlots-availRightArmSlots,
														weaponAndAmmoSlots1);
											return canStripBothArmsOfHardPointTypeF.f(q.laE,q.raE,availEnySlots,enyHP,usedEnySlots1,usedEnyHP1)&&canStripBothArmsOfHardPointTypeF.f(
												q.laB,
												q.raB,
												availBtcSlots,
												btcHP,
												usedBtcSlots1,
												usedBtcHP1)&&canStripBothArmsOfHardPointTypeF.f(q.laM,q.raM,availMslSlots,mslHP,usedMslSlots1,usedMslHP1);
										};
										final CanStripArm canStripRightArmF=(usedEnyHP2,
											usedBtcHP2,
											usedMslHP2,
											usedEnySlots2,
											usedBtcSlots2,
											usedMslSlots2,
											weaponAndAmmoSlots2)->(canStripArmOfHardPointTypeF.f(availRightArmSlots,
												availEnySlots,
												enyHP,
												q.raE,
												usedEnySlots2,
												usedEnyHP2,
												availMechSlots,
												weaponAndAmmoSlots2)&&canStripArmOfHardPointTypeF.f(availRightArmSlots,
													availBtcSlots,
													btcHP,
													q.raB,
													usedBtcSlots2,
													usedBtcHP2,
													availMechSlots,
													weaponAndAmmoSlots2)
												&&canStripArmOfHardPointTypeF.f(availRightArmSlots,
													availMslSlots,
													mslHP,
													q.raM,
													usedMslSlots2,
													usedMslHP2,
													availMechSlots,
													weaponAndAmmoSlots2));
										final CanStripArm canStripLeftArmF=(usedEnyHP3,
											usedBtcHP3,
											usedMslHP3,
											usedEnySlots3,
											usedBtcSlots3,
											usedMslSlots3,
											weaponAndAmmoSlots3)->canStripArmOfHardPointTypeF.f(availLeftArmSlots,
												availEnySlots,
												enyHP,
												q.laE,
												usedEnySlots3,
												usedEnyHP3,
												availMechSlots,
												weaponAndAmmoSlots3)&&canStripArmOfHardPointTypeF.f(availLeftArmSlots,
													availBtcSlots,
													btcHP,
													q.laB,
													usedBtcSlots3,
													usedBtcHP3,
													availMechSlots,
													weaponAndAmmoSlots3)
												&&canStripArmOfHardPointTypeF.f(availLeftArmSlots,availMslSlots,mslHP,q.laM,usedMslSlots3,usedMslHP3,availMechSlots,weaponAndAmmoSlots3);
										final Function<Collection<QuantityC<Weapon>>,LoadoutSlotsAndHardPoints> loadoutHardpointsAndSlotsF=r->{
											final var loadoutSlotsAndHardpoints=new LoadoutSlotsAndHardPoints(0,0,0,0,0,0);
											r.stream().map(t->new HardPointsAndSlotsOfWeaponType(t.k().slots*t.v(),t.v(),t.k().weaponType)).forEach(t->{
												if(t.weaponType.equals(WeaponType.BTC)){
													loadoutSlotsAndHardpoints.btcHp+=t.hardpoints;
													loadoutSlotsAndHardpoints.btcSl+=t.slots;
												}else if(t.weaponType.equals(WeaponType.ENY)){
													loadoutSlotsAndHardpoints.enyHp+=t.hardpoints;
													loadoutSlotsAndHardpoints.enySl+=t.slots;
												}else{
													loadoutSlotsAndHardpoints.mslHp+=t.hardpoints;
													loadoutSlotsAndHardpoints.mslSl+=t.slots;
												}
											});
											return loadoutSlotsAndHardpoints;
										};
										final Function<Collection<QuantityC<Weapon>>,AvailSlotsAndWeight> availWeightAndSlotsF=a->{
											final var loadoutHardpointsAndSlots=loadoutHardpointsAndSlotsF.apply(a);
											final var weaponAndAmmoSlots=weaponAndAmmoSlotsF.apply(a);
											final var canStripRightArm=canStripRightArmF.f(loadoutHardpointsAndSlots.enyHp,
												loadoutHardpointsAndSlots.btcHp,
												loadoutHardpointsAndSlots.mslHp,
												loadoutHardpointsAndSlots.enySl,
												loadoutHardpointsAndSlots.btcSl,
												loadoutHardpointsAndSlots.mslSl,
												weaponAndAmmoSlots);
											final var canStripLeftArm=canStripLeftArmF.f(loadoutHardpointsAndSlots.enyHp,
												loadoutHardpointsAndSlots.btcHp,
												loadoutHardpointsAndSlots.mslHp,
												loadoutHardpointsAndSlots.enySl,
												loadoutHardpointsAndSlots.btcSl,
												loadoutHardpointsAndSlots.mslSl,
												weaponAndAmmoSlots);
											final var canStripBothArms=canStripBothArmsF.f(loadoutHardpointsAndSlots.enyHp,
												loadoutHardpointsAndSlots.btcHp,
												loadoutHardpointsAndSlots.mslHp,
												loadoutHardpointsAndSlots.enySl,
												loadoutHardpointsAndSlots.btcSl,
												loadoutHardpointsAndSlots.mslSl,
												weaponAndAmmoSlots);
											return new AvailSlotsAndWeight(availWeight(armWeight,availableMechWeight,canStripRightArm,canStripLeftArm,canStripBothArms),
												availSlots(availLeftArmSlots,availRightArmSlots,availMechSlots,canStripRightArm,canStripLeftArm,canStripBothArms));
										};
										final var loadoutWeight_Slots_HslGroupFilter=((Predicate<Collection<QuantityC<Weapon>>>)r->{
											final var availWeightAndSlots=availWeightAndSlotsF.apply(r);
											return r.stream()
												.reduce(0f,(subTotalWeaponWeight2,weaponQuantity)->subTotalWeaponWeight2+weaponQuantity.k().weight*weaponQuantity.v(),(a,b)->(a+b))
												+loadoutAmmoWeightHalfCardinalRoundedF.apply(r)<=availWeightAndSlots.availWeight&&weaponSlotsF.apply(r)<=availWeightAndSlots.availSlots-(6
													-hipUpperLowerFootLegSlots)*2
												&&weaponAndAmmoSlotsF.apply(r)<=availWeightAndSlots.availSlots;
										}).and((Predicate<? super Collection<QuantityC<Weapon>>>)r->{
											for(var i=1;i<=15;i++){
												final var hslGroup=i;
												final var smallestHsl=r.stream().filter(t->t.k().hslGroup==hslGroup&&t.k().heatScaleLimit!=0).mapToInt(t->t.k().heatScaleLimit).min();
												if(smallestHsl.isPresent()&&r.stream().filter(t->t.k().hslGroup==hslGroup).reduce(0,(a,b)->a+b.v(),(a,b)->a+b)>smallestHsl.getAsInt()){
													return false;
												}
											}
											return true;
										});
										final LoadoutsOfWeaponType loadoutsOfWeaponTypeF=(type2,hp5)->multiSet0ThroughK(hp5,
											filteredSkilledQuirkedWeaponsSet.stream().filter(r->r.weaponType.equals(type2)).collect(Collectors.toList()),
											((Predicate<Collection<QuantityC<Weapon>>>)t->t.stream()
												.noneMatch(r->r.k().heatScaleLimit==0?false:r.v()>r.k().heatScaleLimit*targetHslGroups)).and((Predicate<? super Collection<QuantityC<
													Weapon>>>)r->(r.stream().reduce(0,(a,b)->a+b.k().slots*b.v(),(a,b)->a+b)<=(type2.equals(WeaponType.BTC)?availBtcSlots:type2.equals(
														WeaponType.ENY)?availEnySlots:availMslSlots))).and(loadoutWeight_Slots_HslGroupFilter));
										final List<Op> loadoutOutputs=cartesianProductof2DSets(cartesianProductof2DSets(loadoutsOfWeaponTypeF.apply(WeaponType.ENY,enyHP),
											loadoutsOfWeaponTypeF.apply(WeaponType.BTC,btcHP).collect(Collectors.toList())).map(r->(Collection<QuantityC<Weapon>>)r.collect(Collectors
												.toList())).filter(loadoutWeight_Slots_HslGroupFilter),loadoutsOfWeaponTypeF.apply(WeaponType.MSL,mslHP).collect(Collectors.toList()))
													.map(r->{
														final var sortedLoadout=r.sorted((a,b)->{
															final var hpdA=a.k().heat/a.k().damage;
															final var hpdB=b.k().heat/b.k().damage;
															return hpdA<hpdB?-1:hpdA==hpdB?0:1;
														}).collect(Collectors.toList());
														final var availWeightAndSlots=availWeightAndSlotsF.apply(sortedLoadout);
														return new LoadoutData(sortedLoadout,
															availWeightAndSlots.availWeight,
															availWeightAndSlots.availSlots,
															addedHeatSinksF.apply(sortedLoadout,availWeightAndSlots.availSlots,availWeightAndSlots.availWeight));
													})
													.filter(r->(r.loadout.stream()
														.reduce(0f,(subTotalWeaponWeight2,weaponQuantity)->subTotalWeaponWeight2+weaponQuantity.k().weight*weaponQuantity.v(),(a,b)->(a+b))
														+loadoutAmmoWeightHalfCardinalRoundedF.apply(r.loadout)<=r.availWeight&&weaponSlotsF.apply(r.loadout)<=r.availSlots-(6
															-hipUpperLowerFootLegSlots)*2
														&&weaponAndAmmoSlotsF.apply(r.loadout)<=r.availSlots))
													.sorted((useEngageInterval?new Comparator<LoadoutData>(){
														@Override public int compare(final LoadoutData a,final LoadoutData b){
															final var heatCapacity=heatCapacityF.apply(a.addedHS);
															final var heatDissipation=heatDissipationF.apply(a.addedHS);
															final var dpePerWeightedResetTimeA=dpePerWeightedResetTimeF.apply(a.loadout,
																heatCapacity,
																heatDissipation,
																engageIntervalF.apply(heatCapacity,remainingCoolingF.f(a.loadout,heatCapacity,heatDissipation),heatDissipation));
															final var heatCapacityB=heatCapacityF.apply(b.addedHS);
															final var heatDissipationB=heatDissipationF.apply(b.addedHS);
															final var dpePerWeightedResetTimeB=dpePerWeightedResetTimeF.apply(b.loadout,
																heatCapacityB,
																heatDissipationB,
																engageIntervalF.apply(heatCapacityB,remainingCoolingF.f(b.loadout,heatCapacityB,heatDissipationB),heatDissipationB));
															return dpePerWeightedResetTimeA<dpePerWeightedResetTimeB?-1:dpePerWeightedResetTimeA==dpePerWeightedResetTimeB?0:1;
														}
													}:(Comparator<LoadoutData>)(a,b)->{
														final var dpeA=dpeF.f(a.loadout,heatCapacityF.apply(a.addedHS),heatDissipationF.apply(a.addedHS));
														final var dpeB=dpeF.f(b.loadout,heatCapacityF.apply(b.addedHS),heatDissipationF.apply(b.addedHS));
														if(dpeA<dpeB){
															return -1;
														}else if(dpeA==dpeB){
															final var remainingCoolingA=remainingCoolingF.f(a.loadout,heatCapacityF.apply(a.addedHS),heatDissipationF.apply(a.addedHS));
															final var remainingCoolingB=remainingCoolingF.f(b.loadout,heatCapacityF.apply(b.addedHS),heatDissipationF.apply(b.addedHS));
															return remainingCoolingA<remainingCoolingB?-1:remainingCoolingA==remainingCoolingB?0:1;
														}else{
															return 1;
														}
													}).reversed())
													.limit(numLoadoutsPerMechConfig)
													.map(r->{
														final var weaponAndAmmoSlots=weaponAndAmmoSlotsF.apply(r.loadout);
														final var loadoutHardpointsAndSlots=loadoutHardpointsAndSlotsF.apply(r.loadout);
														final var usedEnyHP=loadoutHardpointsAndSlots.enyHp;
														final var usedBtcHP=loadoutHardpointsAndSlots.btcHp;
														final var usedMslHP=loadoutHardpointsAndSlots.mslHp;
														final var usedEnySlots=loadoutHardpointsAndSlots.enySl;
														final var usedBtcSlots=loadoutHardpointsAndSlots.btcSl;
														final var usedMslSlots=loadoutHardpointsAndSlots.mslSl;
														final var canStripRightArm=canStripRightArmF.f(usedEnyHP,usedBtcHP,usedMslHP,usedEnySlots,usedBtcSlots,usedMslSlots,weaponAndAmmoSlots);
														final var canStripLeftArm=canStripLeftArmF.f(usedEnyHP,usedBtcHP,usedMslHP,usedEnySlots,usedBtcSlots,usedMslSlots,weaponAndAmmoSlots);
														final var canStripBothArms=canStripBothArmsF.f(usedEnyHP,usedBtcHP,usedMslHP,usedEnySlots,usedBtcSlots,usedMslSlots,weaponAndAmmoSlots);
														final var availSlots=availSlots(availLeftArmSlots,availRightArmSlots,availMechSlots,canStripRightArm,canStripLeftArm,canStripBothArms);
														final var addedHeatsinks=addedHeatSinksF.apply(r.loadout,
															availSlots,
															availWeight(armWeight,availableMechWeight,canStripRightArm,canStripLeftArm,canStripBothArms));
														final var heatDissipation=heatDissipationF.apply(addedHeatsinks);
														final var totalHPS=r.loadout.stream()
															.reduce(0f,(subTotalHPS,entry2)->subTotalHPS+entry2.k().heat*entry2.v()/(entry2.k().cooldown+entry2.k().duration),(a,b)->a+b);
														final var heatCapacity=heatCapacityF.apply(addedHeatsinks);
														final var dpeFormatted=roundToTwoDeci(dpeF.f(r.loadout,heatCapacity,heatDissipation));
														final var remainingCooling=remainingCoolingF.f(r.loadout,heatCapacity,heatDissipation);
														var subRemainingHeatDissipation=heatDissipation;
														var subColdDPS=0f;
														for(final var weapon6:r.loadout){
															final var hps=weapon6.k().heat*weapon6.v()/(weapon6.k().cooldown+weapon6.k().duration);
															final var dps=weapon6.k().damage*weapon6.v()/(weapon6.k().cooldown+weapon6.k().duration);
															if(subRemainingHeatDissipation-hps<=0){
																subColdDPS+=subRemainingHeatDissipation/hps*dps;
																break;
															}
															subColdDPS+=dps;
															subRemainingHeatDissipation-=hps;
														}
														final var engageInter=engageIntervalF.apply(heatCapacity,remainingCooling,heatDissipation);
														final var rating=q.chassis+" "
															+q.variant
															+" "
															+w
															+" engine "
															+e
															+" armor "
															+dpeFormatted
															+" DPE "
															+"rating:"
															+(useEngageInterval?dpePerWeightedResetTimeF.apply(r.loadout,heatCapacity,heatDissipation,engageInter):dpeFormatted)
															+":";
														final var loadoutStats="\n			DPE "+dpeFormatted
															+" hot DPS "
															+roundToTwoDeci(r.loadout.stream()
																.reduce(0f,
																	(subTotalDPS,entry3)->subTotalDPS+entry3.k().damage*entry3.v()/(entry3.k().cooldown+entry3.k().duration),
																	(weaponDPSA,weaponDPSB)->weaponDPSA+weaponDPSB))
															+" fire duration "
															+roundToTwoDeci(totalHPS<heatDissipation?targetEngageDuration
																:(heatCapacity-r.loadout.stream()
																	.reduce(0f,
																		(subTotalHeat,entry4)->subTotalHeat+entry4.k().heat*entry4.v()*(1-entry4.k().jamChance),
																		(weaponHeatA,weaponHeatB)->weaponHeatA+weaponHeatB))/(totalHPS-heatDissipation))
															+" cold DPS "
															+roundToTwoDeci(subColdDPS)
															+"\n"
															+"			added heat sinks "
															+addedHeatsinks
															+" capacity "
															+roundToTwoDeci(heatCapacity)
															+" dissipation "
															+roundToTwoDeci(heatDissipation)
															+"\n"
															+"			post-engage remaining capacity "
															+roundToTwoDeci(remainingCooling)
															+" engage interval "
															+roundToTwoDeci(engageInter)
															+"\n"
															+"			mech slots "
															+availSlots
															+" loadout slots "
															+(weaponAndAmmoSlots+Math.max(addedHeatsinks-(q.omni?0:engineHeatSinkSockets),0)*(q.clan?2:3))
															+(canStripBothArms?" stripping both arms":canStripLeftArm&&canStripRightArm&&availLeftArmSlots<availRightArmSlots||canStripLeftArm
																&&!canStripRightArm?" stripping left arm":canStripRightArm?" stripping right arm":"");
														final var srmAmmoWeight=mslAmmoWeightF.apply(r.loadout,srmFilter);
														final var mrmAmmoWeight=mslAmmoWeightF.apply(r.loadout,mrmFilter);
														final List<Op> weaponOutputs=loadoutWeaponsShotsF.f(r.loadout.stream(),heatCapacity,heatDissipation).map(t->{
															final var weapon=t.weaponEntry.k();
															final var weaponDuplicates=t.weaponEntry.v();
															final var weaponStats="			"+(useExactDPE?""+t.shots.intValue():roundToTwoDeci(t.shots))
																+" shots from "
																+weaponDuplicates
																+" "
																+weapon.name
																+(weapon.weaponType.equals(WeaponType.ENY)?""
																	:" ammo "+(weapon.weaponType.equals(WeaponType.MSL)?weapon.specType==3?srmAmmoWeight:mrmAmmoWeight:weapon.weaponType.equals(
																		WeaponType.BTC)?ammoWeightHalfCardinalRoundedF.apply(weaponAmmoWeightMeasuredF.apply(weapon)*weaponDuplicates):0))
																+" cooldown "
																+roundToTwoDeci(weapon.cooldown)
																+(weapon.duration==0?"":" duration "+roundToTwoDeci(weapon.duration))
																+" HPD "
																+roundToTwoDeci(weapon.heat/t.weaponEntry.k().damage);
															return (Op)()->mechLoadoutsPrintStream.println(weaponStats);
														}).collect(Collectors.toList());
														return (Op)()->{
															mechRatingsPrintStream.println(rating);
															mechLoadoutsPrintStream.println(loadoutStats);
															weaponOutputs.forEach(Op::f);
														};
													})
													.collect(Collectors.toList());
										final var loadoutArmorType="		"+e+" armor";
										return (Op)()->{
											if(loadoutOutputs.size()==0){}else{
												mechLoadoutsPrintStream.print(loadoutArmorType);
												loadoutOutputs.forEach(Op::f);
											}
										};
									})
									.collect(Collectors.toList());
								return (Op)()->{
									mechLoadoutsPrintStream.println("	"+w+" engine");
									mechArmorOutputs.forEach(Op::f);
								};
							})
							.collect(Collectors.toList());
						final var mechStats=q.chassis+" "
							+q.variant
							+" "
							+requiredEngineForTargetSpeed
							+" at "
							+roundToTwoDeci(averageMascSpeedBonusDivisor*engineSpeedCoeff
								*requiredEngineForTargetSpeed
								/(-averageMascSpeedBonusDivisor*speedSkill*q.mechWeight+speedIncMascCoeff*speedSkill*q.mechWeight
									+averageMascSpeedBonusDivisor*q.mechWeight
									-speedIncMascCoeff*q.mechWeight))
							+"kph";
						currentMechIndex++;
						return ()->{
							mechLoadoutsPrintStream.println(mechStats);
							mechEngineOutputs.forEach(Op::f);
						};
					}
				}).collect(Collectors.toList()).forEach(Op::f);
				final var endTime=System.currentTimeMillis();
				loggingStream.println("total time "+(endTime-startTime)/1000f/60f+" minutes");
			}catch(final Exception exception){
				uiFrame.setTitle("MWO Build Generator:error,check log");
				logAndExit.accept("failure during mech processing",exception);
			}
			final var mechRatingsFilePath=outputDirectoryStructure+filePathSeparator+mechRatingsFileName;
			final var mechRatingsLines=safeRead(mechRatingsFilePath,uiFrame);
			try(var mechRatingsOutputStream=new PrintStream(new FileOutputStream(mechRatingsFilePath));){
				mechRatingsLines.stream().sorted(new Comparator<String>(){
					@Override public int compare(final String a,final String b){
						final var ratingA=Float.parseFloat(a.split(":")[1]);
						final var ratingB=Float.parseFloat(b.split(":")[1]);
						return ratingA<ratingB?-1:ratingA==ratingB?0:1;
					}
				}.reversed()).forEach(q->mechRatingsOutputStream.println(q));
			}catch(final Exception exception){
				logAndExit.accept("failed to generate "+mechRatingsFilePath,exception);
			}
		}catch(final Exception exception){
			uiFrame.setTitle("MWO Build Generator:failed to generate log");
			System.err.println("failed to generate log");
			exception.printStackTrace();
			System.exit(-1);
		}
		uiFrame.dispose();
	}
	private static int availSlots(final int availLeftArmSlots,
		final int availRightArmSlots,
		final int availMechSlots,
		final boolean canStripRightArm,
		final boolean canStripLeftArm,
		final boolean canStripBothArms){
		return availMechSlots-(canStripBothArms?availLeftArmSlots+availRightArmSlots:canStripLeftArm&&canStripRightArm&&availLeftArmSlots<availRightArmSlots
			||canStripLeftArm&&!canStripRightArm?availLeftArmSlots
			:canStripLeftArm&&canStripRightArm&&availRightArmSlots<availLeftArmSlots||canStripRightArm&&!canStripLeftArm?availRightArmSlots
			:0);
	}
	private static float availWeight(final float armWeight,
		final float availableMechWeight,
		final boolean canStripRightArm,
		final boolean canStripLeftArm,
		final boolean canStripBothArms){
		return availableMechWeight+(canStripBothArms?armWeight*2:canStripLeftArm||canStripRightArm?armWeight:0);
	}
	private static <T> Stream<Stream<T>> cartesianProductof2DSets(final Stream<Collection<T>> a,final Collection<Collection<T>> b){
		return a.flatMap(q->b.stream().map(w->Stream.concat(q.stream(),w.stream())));
	}
	private static <T> Stream<Collection<QuantityC<T>>> kMultiSet(final List<T> source,
		final int subSetSize,
		final Predicate<Collection<QuantityC<T>>> pred){
		if(subSetSize==0){
			return Stream.of(List.of());
		}
		final var sourceSize=source.size();
		final var chosen=new int[subSetSize+1];
		final var arr=new int[sourceSize];
		final var maxSourceIndex=sourceSize-1;
		final class A{
			Stream<Collection<QuantityC<T>>> f(final int index,final int shiftingStartSourceIndex){
				if(index==subSetSize){
					for(var i=0;i<sourceSize;i++){
						arr[i]=0;
					}
					for(var subSetIndex=0;subSetIndex<subSetSize;subSetIndex++){
						arr[chosen[subSetIndex]]+=1;
					}
					final Collection<QuantityC<T>> collect=new ArrayList<>(subSetSize);
					for(var q=0;q<sourceSize;q++){
						if(arr[q]>0){
							collect.add(new QuantityC<>(source.get(q),arr[q]));
						}
					}
					if(pred.test(collect)){
						return Stream.of(collect);
					}
					return Stream.of();
				}
				return IntStream.rangeClosed(shiftingStartSourceIndex,maxSourceIndex).mapToObj(q->{
					chosen[index]=q;
					return f(index+1,q);
				}).flatMap(q->q);
			}
		}
		return new A().f(0,0);
	}
	private static <A,B> Map<A,B> mapFromFile(final int columnIndex,
		final int linesToSkip,
		final Function<String,A> keyTokenMapper,
		final Function<String,B> valueTokenMapper,
		final List<String> fileLines){
		return fileLines.stream()
			.skip(linesToSkip)
			.collect(Collectors.toMap(q->keyTokenMapper.apply(q.substring(0,q.indexOf("\t"))),q->valueTokenMapper.apply(q.split("\t")[columnIndex])));
	}
	private static <T> Stream<Collection<QuantityC<T>>> multiSet0ThroughK(final int subSetSize,
		final List<T> source,
		final Predicate<Collection<QuantityC<T>>> pred){
		return IntStream.rangeClosed(0,subSetSize).mapToObj(q->kMultiSet(source,q,pred)).flatMap(w->w);
	}
	private static float roundToTwoDeci(final float a){
		return Math.round(a*100)/100f;
	}
	private static List<String> safeRead(final String fileName,final Frame frame){
		final List<String> fileLines;
		try{
			fileLines=Files.readAllLines(Paths.get(fileName));
		}catch(final IOException exception){
			System.err.println("failed to process "+fileName);
			exception.printStackTrace();
			frame.setTitle("MWO Build Generator:error,check log");
			System.exit(-1);
			throw new RuntimeException();
		}
		return fileLines;
	}
	interface AddedHeatsinks{
		int apply(Collection<QuantityC<Weapon>> loadout,int adjAvailSlots,float adjAvailWeight);
	}
	interface AmmoWeightHalfCardinalRounded{
		float apply(float measuredAmmoWeight);
	}
	enum ArmorType{
		ANY,FER,LHT,STD
	}
	interface CanStripArm{
		boolean f(int usedEnyHP,int usedBtcHP,int usedMslHP,int usedEnySlots,int usedBtcSlots,int usedMslSlots,int weaponAndAmmoSlots);
	}
	interface DoubleArmStrip{
		boolean f(int armHPA,int armHPB,int availHPSlots,int hp,int weaponSlots,int usedHP);
	}
	interface DPE{
		float f(Collection<QuantityC<Weapon>> loadout,float heatCapacity,float heatDissipation);
	}
	interface DPEPerWeightedResetTime{
		float apply(Collection<QuantityC<Weapon>> loadout,float heatCap,float heatDis,float engageInterval);
	}
	interface EngageInterval{
		float apply(float heatCap,float remCool,float heatDis);
	}
	interface EngineHeatSinkSocketsF{
		int apply(int requiredEngineForTargetSpeed);
	}
	enum EngineType{
		LHT,STD,XL;
	}
	interface FloatFunction{
		float apply(float a);
	}
	interface FloatOfInt{
		float apply(int addedHeatsinks);
	}
	class LoadoutSlotsAndHardPoints{
		int btcHp;
		int btcSl;
		int enyHp;
		int enySl;
		int mslHp;
		int mslSl;
		LoadoutSlotsAndHardPoints(final int enyHp,final int btcHp,final int mslHp,final int enySl,final int btcSl,final int mslSl){
			this.enyHp=enyHp;
			this.btcHp=btcHp;
			this.mslHp=mslHp;
			this.enySl=enySl;
			this.btcSl=btcSl;
			this.mslSl=mslSl;
		}
		@Override public boolean equals(final Object obj){
			if(this==obj){
				return true;
			}
			if(!(obj instanceof LoadoutSlotsAndHardPoints)){
				return false;
			}
			final var other=(LoadoutSlotsAndHardPoints)obj;
			return btcHp==other.btcHp&&btcSl==other.btcSl&&enyHp==other.enyHp&&enySl==other.enySl&&mslHp==other.mslHp&&mslSl==other.mslSl;
		}
		@Override public int hashCode(){
			return Objects.hash(btcHp,btcSl,enyHp,enySl,mslHp,mslSl);
		}
		@Override public String toString(){
			return "LoadoutSlotsAndHardPoints [btcHp="+btcHp+", btcSl="+btcSl+", enyHp="+enyHp+", enySl="+enySl+", mslHp="+mslHp+", mslSl="+mslSl+"]";
		}
	}
	interface Op{
		void f();
	}
	class PropertyFromString{
		String fileName;
		Properties properties;
		private PropertyFromString(final String fileName,final Properties properties){
			this.fileName=fileName;
			this.properties=properties;
		}
		private boolean getBit(final String key){
			return getString(key).equals("1");
		}
		private Float getFloat(final String key){
			return Float.valueOf(getString(key));
		}
		private Integer getInt(final String key){
			return Integer.valueOf(getString(key));
		}
		private String getString(final String key){
			String ret;
			if(properties.containsKey(key)){
				final var property=properties.getProperty(key);
				ret=property;
			}else{
				throw new RuntimeException("property: "+key+" is missing from "+"fileName");
			}
			return ret;
		}
		private void init(){
			try(InputStream inputStream=new FileInputStream(fileName)){
				properties.load(inputStream);
				inputStream.close();
			}catch(final Exception e){
				e.printStackTrace();
			}
		}
		private float parseFloat(final String key){
			return Float.parseFloat(getString(key));
		}
		private int parseInt(final String key){
			return Integer.parseInt(getString(key));
		}
	}
	record AvailSlotsAndWeight(float availWeight,int availSlots){}
	interface FloatOfWeapon{
		float apply(Weapon a);
	}
	record HardPointsAndSlotsOfWeaponType(int slots,int hardpoints,WeaponType weaponType){}
	interface HardpointSlots{
		int f(int raHp,int rtHp,int ltHp,int laHp,int ctHp,int hHp);
	}
	interface HeatLimitedShots{
		float apply(float remaininCoolingPerHeat,float potentialEngageShots);
	}
	interface IntOfLoadout{
		int apply(Collection<QuantityC<Weapon>> loadout);
	}
	interface LoadoutAmmoWeightHalfCardinalRoundedF{
		float apply(Collection<QuantityC<Weapon>> loadout);
	}
	record LoadoutData(Collection<QuantityC<Weapon>> loadout,float availWeight,int availSlots,int addedHS){}
	interface LoadoutHeatSinkSlots{
		int apply(int addedHeatSinks,Collection<QuantityC<Weapon>> loadout);
	}
	interface LoadoutsOfWeaponType{
		Stream<Collection<QuantityC<Weapon>>> apply(WeaponType a,int hp);
	}
	interface LoadoutWeaponsShots{
		Stream<WeaponShots> f(Stream<QuantityC<Weapon>> weapons,float cap,float dis);
	}
	interface MeasureRampUpsPerEngage{
		float apply(float rampUp,Weapon b);
	}
	record Mech(String chassis,
		String variant,
		boolean clan,
		boolean omni,
		int mechWeight,
		short maxEng,
		boolean masc,
		boolean armYaw,
		boolean leftHand,
		boolean rightHand,
		int raB,
		int raE,
		int raM,
		int rtB,
		int rtE,
		int rtM,
		int ltB,
		int ltE,
		int ltM,
		int laB,
		int laE,
		int laM,
		int ctB,
		int ctE,
		int ctM,
		int hB,
		int hE,
		int hM,
		boolean ecm,
		float jumpDist,
		float maxJumpDist,
		StrType strType,
		ArmorType amrType,
		short bnsAmr,
		short bnsStr,
		float cd,
		float btcCd,
		float enyCd,
		float lsrCd,
		float mslCd,
		float lsrDur,
		float h,
		float hDis,
		float btcH,
		float enyH,
		float lsrH,
		float mslH,
		float rng,
		float btcRng,
		float enyRng,
		float lsrRng,
		float mslRng,
		float ac2Cd,
		float ac5Cd,
		float ac10Cd,
		float ac20Cd,
		float lbxCd,
		float lbx10Cd,
		float uacCd,
		float uac5Cd,
		float uac20Cd,
		float gausCd,
		float mgROF,
		float stdLsrDur,
		float mlCd,
		float mlDur,
		float llCd,
		float erlCd,
		float erlDur,
		float erllCd,
		float plCd,
		float plDur,
		float mplCd,
		float lplCd,
		float hlCd,
		float hlDur,
		float ppcCd,
		float erppcCd,
		float ac20H,
		float uac2H,
		float stdLsrH,
		float mlH,
		float llH,
		float erlH,
		float ermlH,
		float erllH,
		float plH,
		float mplH,
		float lplH,
		float hlH,
		float hmlH,
		float ppcH,
		float erppcH,
		float ac10Rng,
		float uacRng,
		float mgRng,
		float stdLsrRng,
		float mlRng,
		float llRng,
		float erlRng,
		float plRng,
		float mplRng,
		float lplRng,
		float ppcRng,
		float hlRng,
		float uacCh,
		float uac5Ch,
		float uac20Ch,
		int uac20Hsl,
		int mlHsl,
		int erllHsl,
		int lplHsl,
		int hlHsl,
		int ppcFamHsl,
		int ppcHsl,
		int erppcHsl,
		int lppcHsl,
		float racJamRampDownDur,
		float srm4Cd,
		float srmH,
		float srm2H){}
	interface MslAmmoWeight{
		float apply(Collection<QuantityC<Weapon>> loadout,Predicate<Weapon> filter);
	}
	record Q(float cd,float dur,float h,float jc,float jd,int hsl,float rng){}
	record QuantityC<K>(K k,int v){}
	interface RemainingCooling{
		float f(Collection<QuantityC<Weapon>> loadout,float cap,float dis);
	}
	interface SingleArmStrip{
		boolean f(int availArmSlots,int availHPSlots,int hp,int armHP,int totalWeaponSlots,int usedHP,int availMechSlots,int loadoutSlots);
	}
	enum StrType{
		ANY,ENDO,STD
	}
	record Weapon(String name,
		boolean clan,
		WeaponType weaponType,
		boolean continuous,
		float damage,
		float cooldown,
		float jamChance,
		float jamDuration,
		float duration,
		float weight,
		int slots,
		int ammo,
		int xAmmo,
		int salvo,
		float heat,
		int minRange,
		float range,
		float maxRange,
		int heatScaleLimit,
		int hslGroup,
		int specType){}
	record WeaponShots(QuantityC<Weapon> weaponEntry,Float shots){}
	enum WeaponType{
		BTC,ENY,MSL
	}
}