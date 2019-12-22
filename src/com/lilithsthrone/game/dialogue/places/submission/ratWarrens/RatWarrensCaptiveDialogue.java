package com.lilithsthrone.game.dialogue.places.submission.ratWarrens;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.lilithsthrone.game.character.CharacterUtils;
import com.lilithsthrone.game.character.GameCharacter;
import com.lilithsthrone.game.character.attributes.Attribute;
import com.lilithsthrone.game.character.body.CoverableArea;
import com.lilithsthrone.game.character.body.valueEnums.BodyMaterial;
import com.lilithsthrone.game.character.effects.Perk;
import com.lilithsthrone.game.character.effects.StatusEffect;
import com.lilithsthrone.game.character.fetishes.Fetish;
import com.lilithsthrone.game.character.gender.Gender;
import com.lilithsthrone.game.character.npc.NPC;
import com.lilithsthrone.game.character.npc.submission.Murk;
import com.lilithsthrone.game.character.npc.submission.RatGangMember;
import com.lilithsthrone.game.character.npc.submission.RatWarrensCaptive;
import com.lilithsthrone.game.character.npc.submission.Shadow;
import com.lilithsthrone.game.character.npc.submission.Silence;
import com.lilithsthrone.game.character.quests.QuestLine;
import com.lilithsthrone.game.character.race.Subspecies;
import com.lilithsthrone.game.combat.Spell;
import com.lilithsthrone.game.dialogue.DialogueFlagValue;
import com.lilithsthrone.game.dialogue.DialogueNode;
import com.lilithsthrone.game.dialogue.responses.Response;
import com.lilithsthrone.game.dialogue.responses.ResponseCombat;
import com.lilithsthrone.game.dialogue.responses.ResponseSex;
import com.lilithsthrone.game.dialogue.utils.UtilText;
import com.lilithsthrone.game.inventory.InventorySlot;
import com.lilithsthrone.game.inventory.clothing.AbstractClothing;
import com.lilithsthrone.game.inventory.clothing.AbstractClothingType;
import com.lilithsthrone.game.inventory.clothing.ClothingType;
import com.lilithsthrone.game.inventory.enchanting.ItemEffect;
import com.lilithsthrone.game.inventory.enchanting.ItemEffectType;
import com.lilithsthrone.game.inventory.enchanting.TFEssence;
import com.lilithsthrone.game.inventory.enchanting.TFModifier;
import com.lilithsthrone.game.inventory.enchanting.TFPotency;
import com.lilithsthrone.game.sex.InitialSexActionInformation;
import com.lilithsthrone.game.sex.Sex;
import com.lilithsthrone.game.sex.SexAreaOrifice;
import com.lilithsthrone.game.sex.SexAreaPenetration;
import com.lilithsthrone.game.sex.SexControl;
import com.lilithsthrone.game.sex.SexParticipantType;
import com.lilithsthrone.game.sex.SexType;
import com.lilithsthrone.game.sex.managers.SexManagerDefault;
import com.lilithsthrone.game.sex.managers.universal.SMGeneric;
import com.lilithsthrone.game.sex.positions.SexPosition;
import com.lilithsthrone.game.sex.positions.slots.SexSlot;
import com.lilithsthrone.game.sex.positions.slots.SexSlotMilkingStall;
import com.lilithsthrone.game.sex.sexActions.baseActions.FingerPenis;
import com.lilithsthrone.game.sex.sexActions.baseActions.FingerVagina;
import com.lilithsthrone.game.sex.sexActions.baseActions.PenisAnus;
import com.lilithsthrone.game.sex.sexActions.baseActions.PenisMouth;
import com.lilithsthrone.game.sex.sexActions.baseActions.PenisVagina;
import com.lilithsthrone.game.sex.sexActions.baseActions.TongueAnus;
import com.lilithsthrone.game.sex.sexActions.baseActions.TongueVagina;
import com.lilithsthrone.main.Main;
import com.lilithsthrone.utils.Colour;
import com.lilithsthrone.utils.Util;
import com.lilithsthrone.utils.Util.Value;
import com.lilithsthrone.world.WorldType;
import com.lilithsthrone.world.places.PlaceType;

/**
 * Dialogue for when the player (plus companion, if applicable) is taken captive by the rats in the Rat Warrens.
 * This also includes dialogue for when the player (+ companion) is made into Vengar's sex slave.
 * 
 * Stocks loop:
 * TF potion
 * - Swallow
 * 		TF into female milker (about 5 days should fully TF)
 * 			Fetishes
 * 			Femininse & Breasts
 * 			Genitals & bigger breasts
 * 			Lactation & max breasts
 * 			Flavouring & tattoo
 * - Spit
 * 		Ring gag
 * 		If already ring gag, spit is disabled
 * If pregnant and ready to give birth, Silence delivers
 * Sex
 * Night
 * - Escape
 * 		Call for guard and pretend to be choking
 * 			Fight
 * 			Sex
 * 		Use spell to break lock
 * 			Sneak out
 * - Sleep
 * 
 * 
 * @since 0.3.5.5
 * @version 0.3.5.5
 * @author Innoxia
 */
public class RatWarrensCaptiveDialogue {
	
	private static CaptiveInteractionType playerInteraction;
	private static boolean playerMurkSex;
	private static Value<SexSlot, SexType> playerSexType;
	
	public static void applyDailyReset() {
		Main.game.getDialogueFlags().setFlag(DialogueFlagValue.ratWarrensCaptiveCompanionGivenBirth, false);
		Main.game.getDialogueFlags().setFlag(DialogueFlagValue.ratWarrensCaptiveOwnerCompanionSex, false);
		Main.game.getDialogueFlags().setFlag(DialogueFlagValue.ratWarrensCaptiveOwnerSex, false);
	}
	
	private static List<GameCharacter> getCharacters(boolean includeCompanion, boolean includeMilkers) {
		List<GameCharacter> guards = new ArrayList<>();
		guards.addAll(Main.game.getCharactersPresent());
		guards.removeIf(npc -> Main.game.getPlayer().getParty().contains(npc) || (!includeMilkers && (npc instanceof RatWarrensCaptive)));
		Collections.sort(guards, (a, b)->a.getLevel()-b.getLevel());
		if(Main.game.getPlayer().hasCompanions()) {
			guards.add(0, Main.game.getPlayer().getMainCompanion());
		}
		return guards;
	}
	
	private static GameCharacter getMainCompanion() {
		if(Main.game.getPlayer().hasCompanions()) {
			return Main.game.getPlayer().getMainCompanion();
		}
		return null;
	}
	
	private static void spawnRats(int count) {
		List<String> adjectives = new ArrayList<>();
		for(int i=0;i<count;i++) {
			try {
				String[] names = new String[] {"thug", "gangster", "gang-member", "mobster"};
				NPC rat = new RatGangMember(Gender.getGenderFromUserPreferences(i==1, i==2));
					Main.game.addNPC(rat, false);
				rat.setLevel(8-i);
				rat.setLocation(Main.game.getPlayer(), true);
				adjectives.add(CharacterUtils.setGenericName(rat, Util.randomItemFrom(names), adjectives));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	private static void banishRats() {
		for(GameCharacter rat : getCharacters(false, false)) {
			Main.game.banishNPC((NPC) rat);
		}
	}
	
	private static GameCharacter getOwner() {
		return Main.game.getNpc(Murk.class);
	}
	
	private static boolean isCompanionDialogue() {
		return getMainCompanion()!=null;
	}

	public static String equipCollar(GameCharacter character) {
		AbstractClothing collar = AbstractClothingType.generateClothing("innoxia_bdsm_metal_collar", Colour.CLOTHING_BLACK_STEEL, Colour.CLOTHING_STEEL, Colour.CLOTHING_GUNMETAL, false);
		collar.removeEffect(new ItemEffect(ItemEffectType.CLOTHING, TFModifier.CLOTHING_SPECIAL, TFModifier.CLOTHING_ENSLAVEMENT, TFPotency.MINOR_BOOST, 0));
		collar.removeEffect(new ItemEffect(ItemEffectType.CLOTHING, TFModifier.CLOTHING_SPECIAL, TFModifier.CLOTHING_SEALING, TFPotency.MINOR_BOOST, 0));
		collar.addEffect(new ItemEffect(ItemEffectType.CLOTHING, TFModifier.CLOTHING_SPECIAL, TFModifier.CLOTHING_SEALING, TFPotency.MAJOR_DRAIN, 0));
		return character.equipClothingFromNowhere(collar, true, character);
	}
	
	private static String equipRingGag(GameCharacter character) {
		AbstractClothing gag = AbstractClothingType.generateClothing(ClothingType.BDSM_RINGGAG, Colour.CLOTHING_PINK_HOT, Colour.CLOTHING_PINK_LIGHT, Colour.CLOTHING_STEEL, false);
		gag.removeEffect(new ItemEffect(ItemEffectType.CLOTHING, TFModifier.CLOTHING_SPECIAL, TFModifier.CLOTHING_SEALING, TFPotency.BOOST, 0));
		gag.addEffect(new ItemEffect(ItemEffectType.CLOTHING, TFModifier.CLOTHING_SPECIAL, TFModifier.CLOTHING_SEALING, TFPotency.MAJOR_BOOST, 0));
		return character.equipClothingFromNowhere(gag, true, getOwner());
	}
	
	private static String getCompanionTfEffects() {
		StringBuilder sb = new StringBuilder();
		// Companion effects:
		if(isCompanionDialogue()) {
			CaptiveTransformation companionTf = CaptiveTransformation.getTransformationType(getMainCompanion());

			if(companionTf!=null) {
				if(getMainCompanion().isAbleToSelfTransform()
						&& companionTf!=CaptiveTransformation.FEMININE_FETISH
						&& companionTf!=CaptiveTransformation.MASCULINE_FETISH) {
					sb.append(UtilText.parseFromXMLFile("places/submission/ratWarrens/captive", "STOCKS_TRANSFORM_COMPANION_SELF", getCharacters(true, false)));
					
				} else if(getMainCompanion().getFetishDesire(Fetish.FETISH_TRANSFORMATION_RECEIVING).isPositive() || getMainCompanion().getClothingInSlot(InventorySlot.MOUTH)!=null) {
					sb.append(UtilText.parseFromXMLFile("places/submission/ratWarrens/captive", "STOCKS_TRANSFORM_COMPANION_SWALLOW", getCharacters(true, false)));
					
				} else {
					sb.append(UtilText.parseFromXMLFile("places/submission/ratWarrens/captive", "STOCKS_TRANSFORM_COMPANION_REFUSE", getCharacters(true, false)));
					equipRingGag(getMainCompanion());
					return sb.toString(); // Return before applying effects, as this is the variation where your companion spits out the potion. 
				}
				
//				Main.game.getTextStartStringBuilder().append(UtilText.parseFromXMLFile("places/submission/ratWarrens/captive", "STOCKS_TRANSFORM_COMPANION", getCharacters(false)));
				Map<String, String> effects = companionTf.getEffects(getMainCompanion(), true);
				for(Entry<String, String> entry : effects.entrySet()) {
					sb.append(
							"<p>"
								+ UtilText.parse(getOwner(), "[npc.speech("+entry.getKey()+")]")
							+ "</p>"
							+ entry.getValue());
				}
				
			} else {
				if(Main.game.getDialogueFlags().hasFlag(DialogueFlagValue.ratWarrensCaptiveMilkingStartedCompanion)) {
					sb.append(UtilText.parseFromXMLFile("places/submission/ratWarrens/captive", "STOCKS_TRANSFORM_COMPANION_MILKING", getCharacters(true, false)));
					
				} else {
					sb.append(UtilText.parseFromXMLFile("places/submission/ratWarrens/captive", "STOCKS_TRANSFORM_COMPANION_MILKING_START", getCharacters(true, false)));
					Main.game.getDialogueFlags().setFlag(DialogueFlagValue.ratWarrensCaptiveMilkingStartedCompanion, true);
				}
			}
		}
		
		return sb.toString();
	}
	
	private static SexType getRandomSexTypeForCompanion(GameCharacter partner) {
		//TODO if partner!=null, use preferences
		SexType sexType = new SexType(SexParticipantType.NORMAL, SexAreaOrifice.MOUTH, SexAreaPenetration.PENIS);
		float rnd = (float) Math.random();
		if(getMainCompanion().hasVagina() && rnd<0.8f) {
			sexType = new SexType(SexParticipantType.NORMAL, SexAreaOrifice.VAGINA, SexAreaPenetration.PENIS);
		}
		if(Main.game.isAnalContentEnabled() && rnd<0.4f) {
			sexType = new SexType(SexParticipantType.NORMAL, SexAreaOrifice.ANUS, SexAreaPenetration.PENIS);
		}
		if(getMainCompanion().hasPenis() && rnd<0.2f) {
			if(rnd<0.05f) {
				sexType = new SexType(SexParticipantType.NORMAL, SexAreaPenetration.PENIS, SexAreaPenetration.FINGER);
			} else {
				sexType = new SexType(SexParticipantType.NORMAL, SexAreaPenetration.PENIS, SexAreaOrifice.MOUTH);
			}
		}
		return sexType;
	}
	
	private static void calculatePlayerSexType() {
		List<Value<SexSlot, SexType>> sexTypes = new ArrayList<>();
		
		if(Main.game.isAnalContentEnabled()) {
			sexTypes.add(new Value<>(SexSlotMilkingStall.BEHIND_MILKING_STALL, new SexType(SexParticipantType.NORMAL, SexAreaPenetration.PENIS, SexAreaOrifice.ANUS)));
		}
		if(Main.game.getPlayer().hasVagina()) {
			sexTypes.add(new Value<>(SexSlotMilkingStall.BEHIND_MILKING_STALL, new SexType(SexParticipantType.NORMAL, SexAreaPenetration.PENIS, SexAreaOrifice.VAGINA)));
		}
		sexTypes.add(new Value<>(SexSlotMilkingStall.RECEIVING_ORAL, new SexType(SexParticipantType.NORMAL, SexAreaPenetration.PENIS, SexAreaOrifice.MOUTH)));
		
		if(Main.game.isAnalContentEnabled()) {
			if(Main.game.getPlayer().hasFetish(Fetish.FETISH_MASOCHIST)) {
				sexTypes.add(new Value<>(SexSlotMilkingStall.RECEIVING_ORAL, new SexType(SexParticipantType.NORMAL, SexAreaOrifice.ANUS, SexAreaPenetration.TONGUE)));
			}
			sexTypes.add(new Value<>(SexSlotMilkingStall.PERFORMING_ORAL, new SexType(SexParticipantType.NORMAL, SexAreaPenetration.TONGUE, SexAreaOrifice.ANUS)));
		}
		
		if(Main.game.getPlayer().hasPenis()) {
			sexTypes.add(new Value<>(SexSlotMilkingStall.PERFORMING_ORAL, new SexType(SexParticipantType.NORMAL, SexAreaPenetration.FINGER, SexAreaPenetration.PENIS)));
		}
		
		if(Main.game.getPlayer().hasVagina()) {
			sexTypes.add(new Value<>(SexSlotMilkingStall.PERFORMING_ORAL, new SexType(SexParticipantType.NORMAL, SexAreaPenetration.FINGER, SexAreaOrifice.VAGINA)));
			sexTypes.add(new Value<>(SexSlotMilkingStall.PERFORMING_ORAL, new SexType(SexParticipantType.NORMAL, SexAreaPenetration.TONGUE, SexAreaOrifice.VAGINA)));
		}
		
		playerSexType = Util.randomItemFrom(sexTypes);
	}
	
	private static ResponseSex getPlayerOwnerSexResponse(String title, String description, DialogueNode node, String introDialoguePath) {
		return new ResponseSex(title,
				description,
				true,
				false,
				new SexManagerDefault(
						SexPosition.MILKING_STALL,
						Util.newHashMapOfValues(
								new Value<>(getOwner(), playerSexType.getKey())),
						Util.newHashMapOfValues(
								new Value<>(Main.game.getPlayer(), SexSlotMilkingStall.LOCKED_IN_MILKING_STALL))) {
					@Override
					public SexControl getSexControl(GameCharacter character) {
						if(character.isPlayer() || character.equals(getMainCompanion())) {
							return SexControl.NONE;
						}
						return super.getSexControl(character);
					}
					@Override
					public boolean isPositionChangingAllowed(GameCharacter character) {
						return false;
					}
					@Override
					public boolean isAbleToRemoveOthersClothing(GameCharacter character, AbstractClothing clothing) {
						if(character.isPlayer() || character.equals(getMainCompanion())) {
							return false;
						}
						return super.isAbleToRemoveOthersClothing(character, clothing);
					}
					@Override
					public boolean isAbleToEquipSexClothing(GameCharacter character) {
						return !character.isPlayer() && !character.equals(getMainCompanion());
					}
					@Override
					public boolean isAbleToRemoveSelfClothing(GameCharacter character) {
						return !character.isPlayer() && !character.equals(getMainCompanion());
					}
					@Override
					public List<CoverableArea> getAdditionalAreasToExposeDuringSex(GameCharacter performer, GameCharacter target) {
						if(!performer.isPlayer()
								&& (playerSexType.getValue().getPerformingSexArea()==SexAreaPenetration.PENIS || playerSexType.getValue().getPerformingSexArea()==SexAreaOrifice.VAGINA)) {
							return Util.newArrayListOfValues(CoverableArea.PENIS, CoverableArea.VAGINA);
						}
						return new ArrayList<>();
					}
					@Override
					public SexType getForeplayPreference(NPC character, GameCharacter targetedCharacter) {
						if(character.isPlayer()) {
							return super.getForeplayPreference(character, targetedCharacter);
						}
						return playerSexType.getValue();
					}
					@Override
					public SexType getMainSexPreference(NPC character, GameCharacter targetedCharacter) {
						if(character.isPlayer()) {
							return super.getMainSexPreference(character, targetedCharacter);
						}
						return playerSexType.getValue();
					}
					
				},
				null,
				null,
				node,
				UtilText.parseFromXMLFile("places/submission/ratWarrens/captive", introDialoguePath, getCharacters(true, false))) {
			@Override
			public List<InitialSexActionInformation> getInitialSexActions() {
				if(playerSexType.getValue().equals(new SexType(SexParticipantType.NORMAL, SexAreaPenetration.PENIS, SexAreaOrifice.ANUS))) {
					return Util.newArrayListOfValues(new InitialSexActionInformation(getOwner(), Main.game.getPlayer(), PenisAnus.PENIS_FUCKING_START, false, true));
					
				} else if(playerSexType.getValue().equals(new SexType(SexParticipantType.NORMAL, SexAreaPenetration.PENIS, SexAreaOrifice.VAGINA))) {
					return Util.newArrayListOfValues(new InitialSexActionInformation(getOwner(), Main.game.getPlayer(), PenisVagina.PENIS_FUCKING_START, false, true));
					
				} else if(playerSexType.getValue().equals(new SexType(SexParticipantType.NORMAL, SexAreaPenetration.PENIS, SexAreaOrifice.MOUTH))) {
					return Util.newArrayListOfValues(new InitialSexActionInformation(getOwner(), Main.game.getPlayer(), PenisMouth.BLOWJOB_START, false, true));
					
				} else if(playerSexType.getValue().equals(new SexType(SexParticipantType.NORMAL, SexAreaOrifice.ANUS, SexAreaPenetration.TONGUE))) {
					return Util.newArrayListOfValues(new InitialSexActionInformation(getOwner(), Main.game.getPlayer(), TongueAnus.RECEIVING_ANILINGUS_START, false, true));

				} else if(playerSexType.getValue().equals(new SexType(SexParticipantType.NORMAL, SexAreaPenetration.TONGUE, SexAreaOrifice.ANUS))) {
					return Util.newArrayListOfValues(new InitialSexActionInformation(getOwner(), Main.game.getPlayer(), TongueAnus.ANILINGUS_START, false, true));
					
				} else if(playerSexType.getValue().equals(new SexType(SexParticipantType.NORMAL, SexAreaPenetration.FINGER, SexAreaPenetration.PENIS))) {
					return Util.newArrayListOfValues(new InitialSexActionInformation(getOwner(), Main.game.getPlayer(), FingerPenis.COCK_MASTURBATING_START, false, true));
					
				} else if(playerSexType.getValue().equals(new SexType(SexParticipantType.NORMAL, SexAreaPenetration.FINGER, SexAreaOrifice.VAGINA))) {
					return Util.newArrayListOfValues(new InitialSexActionInformation(getOwner(), Main.game.getPlayer(), FingerVagina.FINGERING_START, false, true));
					
				} else if(playerSexType.getValue().equals(new SexType(SexParticipantType.NORMAL, SexAreaPenetration.TONGUE, SexAreaOrifice.VAGINA))) {
					return Util.newArrayListOfValues(new InitialSexActionInformation(getOwner(), Main.game.getPlayer(), TongueVagina.CUNNILINGUS_START, false, true));
				}
				return super.getInitialSexActions();
			}
		};
	}
	

	private static ResponseSex getPlayerMilkingStallSexResponse(String title, String description, DialogueNode node, Map<GameCharacter, SexSlot> npcSlots, String introDialoguePath) {
		return new ResponseSex(title,
				description,
				true,
				false,
				new SexManagerDefault(
						SexPosition.MILKING_STALL,
						npcSlots,
						Util.newHashMapOfValues(
								new Value<>(Main.game.getPlayer(), SexSlotMilkingStall.LOCKED_IN_MILKING_STALL))) {
					@Override
					public SexControl getSexControl(GameCharacter character) {
						if(character.isPlayer() || character.equals(getMainCompanion())) {
							return SexControl.NONE;
						}
						return super.getSexControl(character);
					}
					@Override
					public boolean isPositionChangingAllowed(GameCharacter character) {
						return false;
					}
					@Override
					public boolean isAbleToRemoveOthersClothing(GameCharacter character, AbstractClothing clothing) {
						if(character.isPlayer() || character.equals(getMainCompanion())) {
							return false;
						}
						return super.isAbleToRemoveOthersClothing(character, clothing);
					}
					@Override
					public boolean isAbleToEquipSexClothing(GameCharacter character) {
						return !character.isPlayer() && !character.equals(getMainCompanion());
					}
					@Override
					public boolean isAbleToRemoveSelfClothing(GameCharacter character) {
						return !character.isPlayer() && !character.equals(getMainCompanion());
					}
				},
				null,
				null,
				node,
				UtilText.parseFromXMLFile("places/submission/ratWarrens/captive", introDialoguePath, getCharacters(true, false)));
	}
	
	private static void applyWaitingEffects() {
		Main.game.getTextStartStringBuilder().append(UtilText.parseFromXMLFile("places/submission/ratWarrens/captive", "STOCKS_WAITING_START", getCharacters(true, false)));
		
		getOwner().returnToHome();
		
		if(isCompanionDialogue()) {
			if(getMainCompanion().hasStatusEffect(StatusEffect.PREGNANT_3)) {
				getMainCompanion().endPregnancy(true);
				getMainCompanion().setMana(0);
				
				if(getMainCompanion().getBodyMaterial()!=BodyMaterial.SLIME) {
					getMainCompanion().incrementVaginaStretchedCapacity(15);
					getMainCompanion().incrementVaginaCapacity(
							(getMainCompanion().getVaginaStretchedCapacity()-getMainCompanion().getVaginaRawCapacityValue())*getMainCompanion().getVaginaPlasticity().getCapacityIncreaseModifier(),
							false);
				}
				Main.game.getDialogueFlags().setFlag(DialogueFlagValue.ratWarrensCaptiveCompanionGivenBirth, true);
				Main.game.getTextStartStringBuilder().append(UtilText.parseFromXMLFile("places/submission/ratWarrens/captive", "STOCKS_WAITING_COMPANION_END_PREGNANACY", getCharacters(true, false)));
				
			} else if(!Main.game.getDialogueFlags().hasFlag(DialogueFlagValue.ratWarrensCaptiveCompanionGivenBirth)){ // If given birth today, let them rest
				CaptiveInteractionType companionInteraction = CaptiveInteractionType.getRandomType(getMainCompanion());
				boolean murkSex=false;
				calculatePlayerSexType();
				switch(companionInteraction) {
					case ESSENCE_EXTRACTION:
						// Should be impossible
						break;
					case MILKING:
						getOwner().setLocation(Main.game.getPlayer(), false);
						Main.game.getTextStartStringBuilder().append(UtilText.parseFromXMLFile("places/submission/ratWarrens/captive", "STOCKS_WAITING_COMPANION_MILKING", getCharacters(true, false)));
						murkSex = Math.random()<0.25f && !Main.game.getDialogueFlags().hasFlag(DialogueFlagValue.ratWarrensCaptiveOwnerCompanionSex);
						break;
					case PUNISHMENT:
						getOwner().setLocation(Main.game.getPlayer(), false);
						Main.game.getTextStartStringBuilder().append(UtilText.parseFromXMLFile("places/submission/ratWarrens/captive", "STOCKS_WAITING_COMPANION_PUNISHMENT", getCharacters(true, false)));
						murkSex = Math.random()<0.25f && !Main.game.getDialogueFlags().hasFlag(DialogueFlagValue.ratWarrensCaptiveOwnerCompanionSex);
						break;
					case SEX:
						Main.game.getTextStartStringBuilder().append(UtilText.parseFromXMLFile("places/submission/ratWarrens/captive", "STOCKS_WAITING_COMPANION_SEX", getCharacters(true, false)));
						Main.game.getTextStartStringBuilder().append(getMainCompanion().calculateGenericSexEffects(false, true, null, Subspecies.RAT_MORPH, null, getRandomSexTypeForCompanion(null)));
						break;
					case SEX_THREESOME:
						Main.game.getTextStartStringBuilder().append(UtilText.parseFromXMLFile("places/submission/ratWarrens/captive", "STOCKS_WAITING_COMPANION_SEX_THREESOME", getCharacters(true, false)));
						Main.game.getTextStartStringBuilder().append(getMainCompanion().calculateGenericSexEffects(false, true, null, Subspecies.RAT_MORPH, null, getRandomSexTypeForCompanion(null)));
						Main.game.getTextStartStringBuilder().append(getMainCompanion().calculateGenericSexEffects(false, true, null, Subspecies.RAT_MORPH, null, getRandomSexTypeForCompanion(null)));
						break;
					case TEASE:
						getOwner().setLocation(Main.game.getPlayer(), false);
						Main.game.getTextStartStringBuilder().append(UtilText.parseFromXMLFile("places/submission/ratWarrens/captive", "STOCKS_WAITING_COMPANION_TEASE", getCharacters(true, false)));
						murkSex = Math.random()<0.25f && !Main.game.getDialogueFlags().hasFlag(DialogueFlagValue.ratWarrensCaptiveOwnerCompanionSex);
						break;
				}
				if(murkSex) {
					Main.game.getTextStartStringBuilder().append(UtilText.parseFromXMLFile("places/submission/ratWarrens/captive", "STOCKS_WAITING_COMPANION_MURK_SEX", getCharacters(true, false)));
					Main.game.getTextStartStringBuilder().append(getMainCompanion().calculateGenericSexEffects(false, true, getOwner(), getRandomSexTypeForCompanion(getOwner())));
				}
			}
		}

		getOwner().returnToHome();
		
		playerInteraction = CaptiveInteractionType.getRandomType(Main.game.getPlayer());
		playerMurkSex = false;
		switch(playerInteraction) {
			case ESSENCE_EXTRACTION:
				getOwner().setLocation(Main.game.getPlayer(), false);
				Main.game.getTextStartStringBuilder().append(UtilText.parseFromXMLFile("places/submission/ratWarrens/captive", "STOCKS_WAITING_ESSENCE_EXTRACTION", getCharacters(true, false)));
				Main.game.getTextStartStringBuilder().append(Main.game.getPlayer().setLust(75));
				Main.game.getPlayer().setEssenceCount(TFEssence.ARCANE, 0);
				break;
			case MILKING:
				getOwner().setLocation(Main.game.getPlayer(), false);
				Main.game.getTextStartStringBuilder().append(UtilText.parseFromXMLFile("places/submission/ratWarrens/captive", "STOCKS_WAITING_MILKING", getCharacters(true, false)));
				Main.game.getTextStartStringBuilder().append(Main.game.getPlayer().setLust(75));
				break;
			case PUNISHMENT:
				getOwner().setLocation(Main.game.getPlayer(), false);
				Main.game.getTextStartStringBuilder().append(UtilText.parseFromXMLFile("places/submission/ratWarrens/captive", "STOCKS_WAITING_PUNISHMENT", getCharacters(true, false)));
				Main.game.getTextStartStringBuilder().append(Main.game.getPlayer().setLust(75));
				break;
			case SEX:
				Main.game.getTextStartStringBuilder().append(UtilText.parseFromXMLFile("places/submission/ratWarrens/captive", "STOCKS_WAITING_SEX", getCharacters(false, false)));
				spawnRats(1);
				break;
			case SEX_THREESOME:
				Main.game.getTextStartStringBuilder().append(UtilText.parseFromXMLFile("places/submission/ratWarrens/captive", "STOCKS_WAITING_SEX_THREESOME", getCharacters(false, false)));
				spawnRats(2);
				break;
			case TEASE:
				getOwner().setLocation(Main.game.getPlayer(), false);
				Main.game.getTextStartStringBuilder().append(UtilText.parseFromXMLFile("places/submission/ratWarrens/captive", "STOCKS_WAITING_TEASE", getCharacters(true, false)));
				Main.game.getTextStartStringBuilder().append(Main.game.getPlayer().setLust(95));
				playerMurkSex = Math.random()<0.25f && !Main.game.getDialogueFlags().hasFlag(DialogueFlagValue.ratWarrensCaptiveOwnerSex);
				break;
		}
	}
	
	public static void restoreInventories() {
		Main.game.getPlayer().setInventory(Main.game.getSavedInventories().get(Main.game.getPlayer().getId()));
		if(isCompanionDialogue()) {
			getMainCompanion().setInventory(Main.game.getSavedInventories().get(getMainCompanion().getId()));
		}
	}
	
	public static final DialogueNode STOCKS_INITIAL = new DialogueNode("", "", true) {
		@Override
		public int getSecondsPassed() {
			return 15*60;
		}
		@Override
		public String getContent() {
			return UtilText.parseFromXMLFile("places/submission/ratWarrens/captive", "STOCKS_INITIAL", getCharacters(true, false));
		}
		@Override
		public Response getResponse(int responseTab, int index) {
			if(index==1) {
				return new Response("Sleep",
						(isCompanionDialogue()
							?UtilText.parse(getCharacters(true, false), "Completely exhausted from your fight and the subsequent fucking, you and [npc.name] find yourselves drifting off to sleep...")
							:"Completely exhausted from your fight and the subsequent fucking, you find yourself drifting off to sleep...")
							+ "<br/>[style.italicsTfGeneric(Your 'Forced TF Gender Tendency' content setting determines whether you will be transformed into a feminine or masculine milker during your captivity!)]", //TODO put in main text
						STOCKS_SLEEP);
			}
			return null;
		}
	};
	
	public static final DialogueNode STOCKS_SLEEP = new DialogueNode("", "", true) {
		@Override
		public void applyPreParsingEffects() {
			Main.game.getDialogueFlags().setFlag(DialogueFlagValue.ratWarrensCaptiveCalledOut, false);
		}
		@Override
		public int getSecondsPassed() {
			long secondsOfDay = Main.game.getSecondsPassed()%(60*60*24);
			if(secondsOfDay<(60*60*6)) {
				return (int) ((60*60*6) - secondsOfDay);
			}
			return (int) ((60*60*24) - secondsOfDay) + (60*60*6);
		}
		@Override
		public String getContent() {
			UtilText.nodeContentSB.setLength(0);
			CaptiveTransformation playerTf = CaptiveTransformation.getTransformationType(Main.game.getPlayer());
			
			UtilText.nodeContentSB.append(UtilText.parseFromXMLFile("places/submission/ratWarrens/captive", "STOCKS_SLEEP", getCharacters(true, false)));

			if(!Main.game.getDialogueFlags().hasFlag(DialogueFlagValue.ratWarrensCaptiveTransformationsStarted)) {
				if(playerTf.isFeminine()) {
					UtilText.nodeContentSB.append(UtilText.parseFromXMLFile("places/submission/ratWarrens/captive", "STOCKS_SLEEP_FIRST_DAY_INTRO_FEMININE", getCharacters(true, false)));
				} else {
					UtilText.nodeContentSB.append(UtilText.parseFromXMLFile("places/submission/ratWarrens/captive", "STOCKS_SLEEP_FIRST_DAY_INTRO_MASCULINE", getCharacters(true, false)));
				}
				
			} else {
				if(playerTf!=null) {
					if(Main.game.getPlayer().isAbleToSelfTransform()) { // If possible, they try to make you self-tf:
						UtilText.nodeContentSB.append(UtilText.parseFromXMLFile("places/submission/ratWarrens/captive", "STOCKS_SLEEP_SELF_TF_DEMAND", getCharacters(true, false)));
						
					} else {
						UtilText.nodeContentSB.append(UtilText.parseFromXMLFile("places/submission/ratWarrens/captive", "STOCKS_SLEEP_POTION", getCharacters(true, false)));
					}
					
				} else {
					UtilText.nodeContentSB.append(UtilText.parseFromXMLFile("places/submission/ratWarrens/captive", "STOCKS_SLEEP_MILKING_CHECK", getCharacters(true, false)));
				}
			}
			
			return UtilText.nodeContentSB.toString();
		}
		@Override
		public Response getResponse(int responseTab, int index) {
			CaptiveTransformation playerTf = CaptiveTransformation.getTransformationType(Main.game.getPlayer());
			
			if(!Main.game.getDialogueFlags().hasFlag(DialogueFlagValue.ratWarrensCaptiveTransformationsStarted)) {
				if(!Main.getProperties().getForcedTFTendency().isMasculine()) {
					if(index==1) {
						return new Response("[murk.Name]",
								"Look up at Murk, signalling that you'd prefer to be transformed into a [style.colourFeminine(female)] milker.",
								STOCKS_SLEEP_FIRST_DAY) {
							@Override
							public void effects() {
								Main.game.getDialogueFlags().setFlag(DialogueFlagValue.ratWarrensCaptiveTransformationsStarted, true);
								Main.game.getDialogueFlags().setFlag(DialogueFlagValue.ratWarrensCaptiveFeminine, true);
								Main.game.getTextStartStringBuilder().append(UtilText.parseFromXMLFile("places/submission/ratWarrens/captive", "STOCKS_SLEEP_FIRST_DAY_TF_FEMALE", getCharacters(true, false)));
								if(playerTf==null) {
									Main.game.getTextStartStringBuilder().append(UtilText.parseFromXMLFile("places/submission/ratWarrens/captive", "STOCKS_SLEEP_FIRST_DAY_ALREADY_TF_COMPLETE", getCharacters(true, false)));
								} else {
									if(Main.game.getPlayer().isAbleToSelfTransform()) { // If possible, they try to make you self-tf:
										Main.game.getTextStartStringBuilder().append(UtilText.parseFromXMLFile("places/submission/ratWarrens/captive", "STOCKS_SLEEP_SELF_TF_DEMAND", getCharacters(true, false)));
									} else {
										UtilText.nodeContentSB.append(UtilText.parseFromXMLFile("places/submission/ratWarrens/captive", "STOCKS_SLEEP_POTION", getCharacters(true, false)));
									}
								}
							}
						};
						
					} else if(index==2) {
						return new Response("Rat-girl",
								"Look up at the rat-girl, signalling that you'd prefer to be transformed into a [style.colourFeminineStrong(futa)] milker.",
								STOCKS_SLEEP_FIRST_DAY) {
							@Override
							public void effects() {
								Main.game.getDialogueFlags().setFlag(DialogueFlagValue.ratWarrensCaptiveTransformationsStarted, true);
								Main.game.getDialogueFlags().setFlag(DialogueFlagValue.ratWarrensCaptiveFuta, true);
								Main.game.getTextStartStringBuilder().append(UtilText.parseFromXMLFile("places/submission/ratWarrens/captive", "STOCKS_SLEEP_FIRST_DAY_TF_FUTA", getCharacters(true, false)));
								if(playerTf==null) {
									Main.game.getTextStartStringBuilder().append(UtilText.parseFromXMLFile("places/submission/ratWarrens/captive", "STOCKS_SLEEP_FIRST_DAY_ALREADY_TF_COMPLETE", getCharacters(true, false)));
								} else {
									if(Main.game.getPlayer().isAbleToSelfTransform()) { // If possible, they try to make you self-tf:
										Main.game.getTextStartStringBuilder().append(UtilText.parseFromXMLFile("places/submission/ratWarrens/captive", "STOCKS_SLEEP_SELF_TF_DEMAND", getCharacters(true, false)));
									} else {
										UtilText.nodeContentSB.append(UtilText.parseFromXMLFile("places/submission/ratWarrens/captive", "STOCKS_SLEEP_POTION", getCharacters(true, false)));
									}
								}
							}
						};
					}
					
				} else {
					if(index==1) {
						return new Response("Pull away",
								"Pull away from [murk.namePos] cock, which will make him want to transform you into a [style.colourMasculineStrong(masculine)] cum-milker.",
								STOCKS_SLEEP_FIRST_DAY) {
							@Override
							public void effects() {
								Main.game.getDialogueFlags().setFlag(DialogueFlagValue.ratWarrensCaptiveTransformationsStarted, true);
								Main.game.getDialogueFlags().setFlag(DialogueFlagValue.ratWarrensCaptiveMasculine, true);
								Main.game.getTextStartStringBuilder().append(UtilText.parseFromXMLFile("places/submission/ratWarrens/captive", "STOCKS_SLEEP_FIRST_DAY_TF_MASCULINE", getCharacters(true, false)));
								if(playerTf==null) {
									Main.game.getTextStartStringBuilder().append(UtilText.parseFromXMLFile("places/submission/ratWarrens/captive", "STOCKS_SLEEP_FIRST_DAY_ALREADY_TF_COMPLETE", getCharacters(true, false)));
								} else {
									if(Main.game.getPlayer().isAbleToSelfTransform()) { // If possible, they try to make you self-tf:
										Main.game.getTextStartStringBuilder().append(UtilText.parseFromXMLFile("places/submission/ratWarrens/captive", "STOCKS_SLEEP_SELF_TF_DEMAND", getCharacters(true, false)));
									} else {
										UtilText.nodeContentSB.append(UtilText.parseFromXMLFile("places/submission/ratWarrens/captive", "STOCKS_SLEEP_POTION", getCharacters(true, false)));
									}
								}
							}
						};
						
					} else if(index==2) {
						return new Response("Push back",
								"Raise your hips and push your ass back into [murk.namePos] cock, which will make him want to transform you into an [style.colourAndrogynous(androgynous)] sissy cum-milker.",
								STOCKS_SLEEP_FIRST_DAY) {
							@Override
							public void effects() {
								Main.game.getDialogueFlags().setFlag(DialogueFlagValue.ratWarrensCaptiveTransformationsStarted, true);
								Main.game.getDialogueFlags().setFlag(DialogueFlagValue.ratWarrensCaptiveSissy, true);
								Main.game.getTextStartStringBuilder().append(UtilText.parseFromXMLFile("places/submission/ratWarrens/captive", "STOCKS_SLEEP_FIRST_DAY_TF_SISSY", getCharacters(true, false)));
								if(playerTf==null) {
									Main.game.getTextStartStringBuilder().append(UtilText.parseFromXMLFile("places/submission/ratWarrens/captive", "STOCKS_SLEEP_FIRST_DAY_ALREADY_TF_COMPLETE", getCharacters(true, false)));
								} else {
									if(Main.game.getPlayer().isAbleToSelfTransform()) { // If possible, they try to make you self-tf:
										Main.game.getTextStartStringBuilder().append(UtilText.parseFromXMLFile("places/submission/ratWarrens/captive", "STOCKS_SLEEP_SELF_TF_DEMAND", getCharacters(true, false)));
									} else {
										UtilText.nodeContentSB.append(UtilText.parseFromXMLFile("places/submission/ratWarrens/captive", "STOCKS_SLEEP_POTION", getCharacters(true, false)));
									}
								}
							}
						};
					}
				}
				
			} else {
				if(playerTf!=null) {
					if(Main.game.getPlayer().isAbleToSelfTransform()
							&& playerTf!=CaptiveTransformation.FEMININE_FETISH
							&& playerTf!=CaptiveTransformation.MASCULINE_FETISH) {
						if(index==1) {
							return new Response("Self-transform",
									"As you've been given the self-transformation fetish, you can't help but contain your excitement at the prospect of being allowed to transform yourself...",
									STOCKS_TRANSFORM) {
								@Override
								public void effects() {
									Main.game.getTextStartStringBuilder().append(UtilText.parseFromXMLFile("places/submission/ratWarrens/captive", "STOCKS_TRANSFORM_SELF", getCharacters(true, false)));
									Map<String, String> effects = playerTf.getEffects(Main.game.getPlayer(), true);
									for(Entry<String, String> entry : effects.entrySet()) {
										Main.game.getTextStartStringBuilder().append(
												"<p>"
													+ UtilText.parse(getOwner(), "[npc.speech("+entry.getKey()+")]")
												+ "</p>"
												+ entry.getValue());
									}
									Main.game.getTextStartStringBuilder().append(getCompanionTfEffects());
								}
							};
						}
						
					} else {
						if(index==1) {
							return new Response("Swallow", "Do as Murk says and swallow the potion...", STOCKS_TRANSFORM) {
								@Override
								public void effects() {
									Main.game.getTextStartStringBuilder().append(UtilText.parseFromXMLFile("places/submission/ratWarrens/captive", "STOCKS_TRANSFORM_SWALLOW", getCharacters(true, false)));
									Map<String, String> effects = playerTf.getEffects(Main.game.getPlayer(), true);
									for(Entry<String, String> entry : effects.entrySet()) {
										Main.game.getTextStartStringBuilder().append(
												"<p>"
													+ UtilText.parse(getOwner(), "[npc.speech("+entry.getKey()+")]")
												+ "</p>"
												+ entry.getValue());
									}
									Main.game.getTextStartStringBuilder().append(getCompanionTfEffects());
								}
							};
							
						} else if(index==2) {
							if(Main.game.isSpittingDisabled()) {
								return Response.getDisallowedSpittingResponse();
							}
							if(Main.game.getPlayer().getClothingInSlot(InventorySlot.MOUTH)!=null) {
								return new Response("Spit", "Due to the fact that you've been forced to wear a ring gag, you can't do anything to stop Murk from pouring the liquid down your throat...", null);
							}
							if(Main.game.getPlayer().hasFetish(Fetish.FETISH_TRANSFORMATION_RECEIVING)) {
								return new Response("Spit",
										"Due to your <b style='color:"+Colour.FETISH.toWebHexString()+";'>"+Fetish.FETISH_TRANSFORMATION_RECEIVING.getName(Main.game.getPlayer())
											+"</b> fetish, you love being transformed so much that you can't bring yourself to spit out the transformative liquid!",
										null);
							}
							return new Response("Spit", "Spit out the transformation potion!", STOCKS_TRANSFORM) {
								@Override
								public void effects() {
									equipRingGag(Main.game.getPlayer());
									Main.game.getTextStartStringBuilder().append(UtilText.parseFromXMLFile("places/submission/ratWarrens/captive", "STOCKS_TRANSFORM_REFUSE", getCharacters(true, false)));
									Main.game.getTextStartStringBuilder().append(getCompanionTfEffects());
								}
							};
						}
					}
					
				} else {
					if(index==1) {
						return new Response("Milked", UtilText.parse(getOwner(), "[npc.Name] hooks you back up to the milking machine..."), STOCKS_TRANSFORM) {
							@Override
							public void effects() {
								Main.game.getTextStartStringBuilder().append(UtilText.parseFromXMLFile("places/submission/ratWarrens/captive", "STOCKS_TRANSFORM_MILKING", getCharacters(true, false)));
								Main.game.getTextStartStringBuilder().append(getCompanionTfEffects());
							}
						};
					}
				}
				
			}
			return null;
		}
	};
	
	public static final DialogueNode STOCKS_SLEEP_FIRST_DAY = new DialogueNode("", "", true) {
		@Override
		public int getSecondsPassed() {
			return 10*60;
		}
		@Override
		public String getContent() {
			return "";
		}
		@Override
		public Response getResponse(int responseTab, int index) {
			return STOCKS_SLEEP.getResponse(responseTab, index);
		}
	};
	
	public static final DialogueNode STOCKS_TRANSFORM = new DialogueNode("", "", true) {
		@Override
		public int getSecondsPassed() {
			return 10*60;
		}
		@Override
		public String getContent() {
			return "";
		}
		@Override
		public Response getResponse(int responseTab, int index) {
			if(!Main.game.getDialogueFlags().hasFlag(DialogueFlagValue.ratWarrensCaptiveMilkingStarted)) {
				if(index==1) {
					return new Response("Milked", UtilText.parse(getOwner(), "[npc.Name] hooks you up to the milking machine..."), STOCKS_TRANSFORM) {
						@Override
						public void effects() {
							Main.game.getTextStartStringBuilder().append(UtilText.parseFromXMLFile("places/submission/ratWarrens/captive", "STOCKS_TRANSFORM_MILKING_START", getCharacters(true, false)));
							Main.game.getDialogueFlags().setFlag(DialogueFlagValue.ratWarrensCaptiveMilkingStarted, true);
						}
					};
				}
				
			} else if(Main.game.getPlayer().hasStatusEffect(StatusEffect.PREGNANT_3)) {
				// Silence delivers
				if(index==1) {
					return new Response("Birthing", UtilText.parse(getOwner(), "[npc.Name] notices that you're ready to give birth..."), STOCKS_GIVE_BIRTH) {
						@Override
						public void effects() {
							Main.game.getNpc(Silence.class).setLocation(Main.game.getPlayer(), false);
							
							Main.game.getPlayer().endPregnancy(true);
							Main.game.getPlayer().setMana(0);
							
							if(Main.game.getPlayer().getBodyMaterial()!=BodyMaterial.SLIME) {
								Main.game.getPlayer().incrementVaginaStretchedCapacity(15);
								Main.game.getPlayer().incrementVaginaCapacity(
										(Main.game.getPlayer().getVaginaStretchedCapacity()-Main.game.getPlayer().getVaginaRawCapacityValue())*Main.game.getPlayer().getVaginaPlasticity().getCapacityIncreaseModifier(),
										false);
							}
							
							if(!Main.game.getPlayer().isQuestCompleted(QuestLine.SIDE_FIRST_TIME_PREGNANCY)) { // If birthing side quest is not complete, remove it, as otherwise completion (referencing Lily) doens't make any sense.
								Main.game.getPlayer().removeQuest(QuestLine.SIDE_FIRST_TIME_PREGNANCY);
							}
						}
					};
				}
				
			} else if(index==1) {
				return new Response("Wait",
						"You can't do anything about your situation at the moment, other than wait to see what happens to you next...",
						Main.game.isExtendedWorkTime()
							?STOCKS_WAITING
							:STOCKS_NIGHT) {
					@Override
					public void effects() {
						if(Main.game.isExtendedWorkTime()) {
							applyWaitingEffects();
						}
					}
				};
			}
			return null;
		}
	};
	
	
	public static final DialogueNode STOCKS_GIVE_BIRTH = new DialogueNode("", "", true) {
		@Override
		public int getSecondsPassed() {
			return 1*60*60;
		}
		@Override
		public String getContent() {
			if(Main.game.getPlayer().getVaginaType().isEggLayer()) {
				return UtilText.parseFromXMLFile("places/submission/ratWarrens/captive", "STOCKS_GIVE_BIRTH_EGGS", getCharacters(true, false));
			} else {
				return UtilText.parseFromXMLFile("places/submission/ratWarrens/captive", "STOCKS_GIVE_BIRTH", getCharacters(true, false));
			}
		}
		@Override
		public Response getResponse(int responseTab, int index) {
			if(index==1) {
				if(Main.game.getPlayer().getVaginaType().isEggLayer()) {
					return new Response("Protect the eggs!", "You spend some time recovering from your ordeal...", STOCKS_GIVE_BIRTH_PROTECT_THE_EGGS) {
						@Override
						public void effects() {
							Main.game.getNpc(Silence.class).returnToHome();
						}
					};
				} else {
					return new Response("Rest", "You spend some time recovering from your ordeal...", STOCKS_GIVE_BIRTH_FINISHED) {
						@Override
						public void effects() {
							Main.game.getNpc(Silence.class).returnToHome();
						}
					};
				}
			}
			return null;
		}
	};
	
	public static final DialogueNode STOCKS_GIVE_BIRTH_PROTECT_THE_EGGS = new DialogueNode("", "", true) {
		@Override
		public int getSecondsPassed() {
			return 12*60*60;
		}
		@Override
		public String getContent() {
			return UtilText.parseFromXMLFile("places/submission/ratWarrens/captive", "STOCKS_GIVE_BIRTH_PROTECT_THE_EGGS", getCharacters(true, false));
		}
		@Override
		public Response getResponse(int responseTab, int index) {
			if(index==1) {
				return new Response("Rest", "You spend some time recovering from your ordeal...", STOCKS_GIVE_BIRTH_FINISHED);
			}
			return null;
		}
	};

	public static final DialogueNode STOCKS_GIVE_BIRTH_FINISHED = new DialogueNode("", "", true) { //TODO append offspring.
		@Override
		public int getSecondsPassed() {
			return 30*60;
		}
		@Override
		public String getContent() {
			if(Main.game.getPlayer().getVaginaType().isEggLayer()) {
				return UtilText.parseFromXMLFile("places/submission/ratWarrens/captive", "STOCKS_GIVE_BIRTH_FINISHED_EGGS", getCharacters(true, false));
			} else {
				return UtilText.parseFromXMLFile("places/submission/ratWarrens/captive", "STOCKS_GIVE_BIRTH_FINISHED", getCharacters(true, false));
			}
		}
		@Override
		public Response getResponse(int responseTab, int index) {
			return STOCKS_AFTER_SEX.getResponse(responseTab, index);
		}
	};
	
	public static final DialogueNode STOCKS_WAITING = new DialogueNode("", "", true) {
		@Override
		public int getSecondsPassed() {
			return (1+Util.random.nextInt(5))*60*60;
		}
		@Override
		public String getContent() {
			return "";
		}
		@Override
		public Response getResponse(int responseTab, int index) {
			switch(playerInteraction) {
				case ESSENCE_EXTRACTION:
				case MILKING:
				case PUNISHMENT:
					if(index==1) {
						return new Response("Wait",
								UtilText.parse(getOwner(), "Having had [npc.her] fun, [npc.name] heads back to the milk storage room, leaving you to wait in the stocks..."),
								Main.game.isExtendedWorkTime()
									?STOCKS_WAITING
									:STOCKS_NIGHT) {
							@Override
							public void effects() {
								Main.game.getTextStartStringBuilder().append(UtilText.parseFromXMLFile("places/submission/ratWarrens/captive", "STOCKS_WAITING_MURK_LEAVES", getCharacters(true, false)));
								if(Main.game.isExtendedWorkTime()) {
									applyWaitingEffects();
								}
							}
						};
					}
					break;
				case TEASE:
					if(index==1) {
						return new Response("Refuse",
								UtilText.parse(getOwner(), "Refuse to beg for [npc.name] to fuck you, and instead stay quiet and wait for [npc.herHim] to leave..."),
								Main.game.isExtendedWorkTime()
									?STOCKS_WAITING
									:STOCKS_NIGHT) {
							@Override
							public void effects() {
								Main.game.getTextStartStringBuilder().append(UtilText.parseFromXMLFile("places/submission/ratWarrens/captive", "STOCKS_WAITING_REFUSE_TO_BEG", getCharacters(true, false)));
								if(Main.game.isExtendedWorkTime()) {
									applyWaitingEffects();
								}
							}
						};
						
					} else if(index==2 && playerMurkSex && playerInteraction==CaptiveInteractionType.TEASE) {
						String introDescriptionPath = "BEG_FOR_SEX";
						if(playerSexType.getValue().equals(new SexType(SexParticipantType.NORMAL, SexAreaPenetration.PENIS, SexAreaOrifice.ANUS))) {
							introDescriptionPath = "BEG_FOR_SEX_ANAL";
							
						} else if(playerSexType.getValue().equals(new SexType(SexParticipantType.NORMAL, SexAreaPenetration.PENIS, SexAreaOrifice.VAGINA))) {
							introDescriptionPath = "BEG_FOR_SEX_VAGINAL";
							
						} else if(playerSexType.getValue().equals(new SexType(SexParticipantType.NORMAL, SexAreaPenetration.PENIS, SexAreaOrifice.MOUTH))) {
							introDescriptionPath = "BEG_FOR_SEX_BLOWJOB";
							
						} else if(playerSexType.getValue().equals(new SexType(SexParticipantType.NORMAL, SexAreaOrifice.ANUS, SexAreaPenetration.TONGUE))) {
							introDescriptionPath = "BEG_FOR_SEX_ANILINGUS";
							
						} else if(playerSexType.getValue().equals(new SexType(SexParticipantType.NORMAL, SexAreaPenetration.TONGUE, SexAreaOrifice.ANUS))) {
							introDescriptionPath = "BEG_FOR_SEX_RECEIVE_ANILINGUS";
							
						} else if(playerSexType.getValue().equals(new SexType(SexParticipantType.NORMAL, SexAreaPenetration.FINGER, SexAreaPenetration.PENIS))) {
							introDescriptionPath = "BEG_FOR_SEX_RECEIVE_HANDJOB";
							
						} else if(playerSexType.getValue().equals(new SexType(SexParticipantType.NORMAL, SexAreaPenetration.FINGER, SexAreaOrifice.VAGINA))) {
							introDescriptionPath = "BEG_FOR_SEX_RECEIVE_FINGERING";
							
						} else if(playerSexType.getValue().equals(new SexType(SexParticipantType.NORMAL, SexAreaPenetration.TONGUE, SexAreaOrifice.VAGINA))) {
							introDescriptionPath = "BEG_FOR_SEX_RECEIVE_CUNNILINGUS";
						}
						return getPlayerOwnerSexResponse("Beg for it", UtilText.parse(getOwner(), "Now that [npc.name] has turned you on so much, you can't help but beg for [npc.herHim] to fuck you!"), STOCKS_AFTER_SEX, introDescriptionPath);
					}
					break;
				case SEX:
					if(index==1) {
						return getPlayerMilkingStallSexResponse("Fucked",
								UtilText.parse(getCharacters(false, false).get(0), "There's nothing you can do to stop the rat from having [npc.her] way with you..."),
								STOCKS_AFTER_SEX, 
								Util.newHashMapOfValues(
										new Value<>(getCharacters(false, false).get(0), SexSlotMilkingStall.BEHIND_MILKING_STALL)), //TODO improve slot?
								"STOCKS_WAITING_SOLO_SEX");
					}
					break;
				case SEX_THREESOME:
					if(index==1) {
						return getPlayerMilkingStallSexResponse("Fucked",
								UtilText.parse(getCharacters(false, false).get(0), "There's nothing you can do to stop the rat from having [npc.her] way with you..."),
								STOCKS_AFTER_SEX, 
								Util.newHashMapOfValues(
										new Value<>(getCharacters(false, false).get(0), SexSlotMilkingStall.BEHIND_MILKING_STALL),
										new Value<>(getCharacters(false, false).get(1), SexSlotMilkingStall.RECEIVING_ORAL)), //TODO improve slots?
								"STOCKS_WAITING_THREESOME_SEX");
					}
					break;
			}
			return null;
		}
	};
	
	public static final DialogueNode STOCKS_AFTER_SEX = new DialogueNode("Finished", "", true) {
		@Override
		public String getDescription() {
			if(Sex.getDominantParticipants(false).containsKey(getOwner())) {
				return UtilText.parse(getOwner(), "[npc.NameHasFull] finished with you for now...");
			}
			if(Sex.getDominantParticipants(false).size()>1) {
				return "The rats have finished with you...";
			}
			return UtilText.parse(getCharacters(false, false), "[npc.Name] has finished with you...");
		}
		@Override
		public int getSecondsPassed() {
			return 10*60;
		}
		@Override
		public String getContent() {
			if(Sex.getDominantParticipants(false).containsKey(getOwner())) {
				return UtilText.parseFromXMLFile("places/submission/ratWarrens/captive", "STOCKS_AFTER_SEX_MURK", getCharacters(false, false));
				
			} else if(Sex.getDominantParticipants(false).size()>1) {
				return UtilText.parseFromXMLFile("places/submission/ratWarrens/captive", "STOCKS_AFTER_SEX_THREESOME", getCharacters(false, false));
				
			}
			return UtilText.parseFromXMLFile("places/submission/ratWarrens/captive", "STOCKS_AFTER_SEX_SOLO", getCharacters(false, false));
		}
		@Override
		public Response getResponse(int responseTab, int index) {
			if(index==1) {
				return new Response("Wait",
						"You can't do anything about your situation at the moment, other than wait to see what happens to you next...",
						Main.game.isExtendedWorkTime()
							?STOCKS_WAITING
							:STOCKS_NIGHT) {
					@Override
					public void effects() {
						banishRats();
						if(Sex.getDominantParticipants(false).containsKey(getOwner())) {
							Main.game.getDialogueFlags().setFlag(DialogueFlagValue.ratWarrensCaptiveOwnerSex, true);
						}
						if(Main.game.isExtendedWorkTime()) {
							applyWaitingEffects();
						}
					}
				};
			}
			return null;
		}
	};

	public static final DialogueNode STOCKS_NIGHT = new DialogueNode("", "", false) {
		//TODO prevent all fast travel
		@Override
		public int getSecondsPassed() {
			return 30*60;
		}
		@Override
		public String getContent() {
			return UtilText.parseFromXMLFile("places/submission/ratWarrens/captive", "STOCKS_NIGHT", getCharacters(true, false));
		}
		@Override
		public Response getResponse(int responseTab, int index) {
			if(index==1) {
				return new Response("Sleep", "Fall asleep...", STOCKS_SLEEP);
				
			} else if(index==2) {
				if(Main.game.getDialogueFlags().hasFlag(DialogueFlagValue.ratWarrensCaptiveCalledOut)) {
					return new Response("Call out", UtilText.parse(getOwner(), "[npc.Name] won't pay attention to you if you call out again. You'll have to wait until tomorrow night..."), null);
				}
				return new Response("Call out", "Call out for [npc.name]...", STOCKS_CALL_OUT);
				
			} else if(index==3) {
				if(Main.game.getPlayer().getAttributeValue(Attribute.MAJOR_PHYSIQUE)>=80) {
					return new Response("Break lock", "Use your raw physical power to break free of the lock...", STOCKS_BROKEN_FREE) {
						@Override
						public void effects() {
							Main.game.getTextStartStringBuilder().append(UtilText.parseFromXMLFile("places/submission/ratWarrens/captive", "STOCKS_NIGHT_BREAK_LOCK", getCharacters(true, false)));
							Main.game.getDialogueFlags().setFlag(DialogueFlagValue.ratWarrensCaptiveAttemptingEscape, true);
						}
					};
				}
				return new Response("Break lock", "You are not strong enough to break the lock...<br/>[style.italicsMinorBad(Required at least 80 "+Attribute.MAJOR_PHYSIQUE.getName()+"...)]", null);
				
			} else if(index==4) {
				if(Main.game.getPlayer().hasSpell(Spell.FIREBALL)
						|| Main.game.getPlayer().hasSpell(Spell.ICE_SHARD)
						|| Main.game.getPlayer().hasSpell(Spell.SLAM)) {
					return new Response("Break lock (Spell)",
							"Spend some time channelling your arcane power in an attempt to overcome your slave collar's enchantment and cast a spell to break the lock on the stocks.",
							STOCKS_BROKEN_FREE) {
						@Override
						public void effects() {
							Main.game.getTextStartStringBuilder().append(UtilText.parseFromXMLFile("places/submission/ratWarrens/captive", "STOCKS_NIGHT_BREAK_LOCK_SPELL", getCharacters(true, false)));
							Main.game.getDialogueFlags().setFlag(DialogueFlagValue.ratWarrensCaptiveAttemptingEscape, true);
						}
					};
				}
				return new Response("Break lock",
						"You do not know a spell that would be suitable to break the lock..."
								+ "<br/>[style.italicsMinorBad(Requires knowing one of the following spells: "+Spell.FIREBALL.getName()+"; "+Spell.ICE_SHARD.getName()+"; "+Spell.SLAM.getName()+".)]",
						null);
			}
			return null;
		}
	};
	
	public static final DialogueNode STOCKS_BROKEN_FREE = new DialogueNode("", "", true) {
		@Override
		public int getSecondsPassed() {
			return 5*60;
		}
		@Override
		public String getContent() {
			return UtilText.parseFromXMLFile("places/submission/ratWarrens/captive", "STOCKS_BROKEN_FREE", getCharacters(true, false));
		}
		@Override
		public Response getResponse(int responseTab, int index) {
			if(index==1) {
				return new ResponseCombat("Fight",
						UtilText.parse(getOwner(), "Now that you're free of your chain, you can finally attack [npc.name]!"),
						(NPC) getOwner(),
						Util.newHashMapOfValues(new Value<>(getOwner(), "[npc.speech(Yer gonna pay fer this!)] [npc.name] shouts as [npc.she] prepares to fight you."))) {
					@Override
					public void effects() {
						Main.game.getDialogueFlags().setFlag(DialogueFlagValue.ratWarrensCaptiveAttemptingEscape, true);
					}
				};
				
			} else if(index==2) {
				return new ResponseSex("Submit",
						UtilText.parse(getOwner(), "Do as [npc.name] says and submit to [npc.herHim]..."),
						Util.newArrayListOfValues(Fetish.FETISH_SUBMISSIVE), null, Fetish.FETISH_SUBMISSIVE.getAssociatedCorruptionLevel(),
						null, null, null,
						true, false,
						new SMGeneric(
								Util.newArrayListOfValues(getOwner()),
								Util.newArrayListOfValues(Main.game.getPlayer()),
								null,
								Main.game.getPlayer().getCompanions()) {
							@Override
							public SexControl getSexControl(GameCharacter character) {
								if(character.isPlayer()) {
									return SexControl.ONGOING_PLUS_LIMITED_PENETRATIONS;
								}
								return super.getSexControl(character);
							}
						},
						STOCKS_RELEASED_AFTER_SEX,
						UtilText.parseFromXMLFile("places/submission/ratWarrens/captive", "STOCKS_BROKEN_FREE_SUBMIT", getCharacters(true, false)));
			}
			return null;
		}
	};

	public static final DialogueNode STOCKS_CALL_OUT = new DialogueNode("", "", true) {
		@Override
		public int getSecondsPassed() {
			return 10*60;
		}
		@Override
		public String getContent() {
			return UtilText.parseFromXMLFile("places/submission/ratWarrens/captive", "STOCKS_CALL_OUT", getCharacters(true, false));
		}
		@Override
		public Response getResponse(int responseTab, int index) {
			if(index==1) {
				return new Response("Feign choking", UtilText.parse(getOwner(), "Pretend to be choking in order to trick [npc.name] into releasing you..."), STOCKS_CALL_OUT_RELEASED) {
					@Override
					public void effects() {
						Main.game.getTextStartStringBuilder().append(UtilText.parseFromXMLFile("places/submission/ratWarrens/captive", "STOCKS_CALL_OUT_CHOKE", getCharacters(true, false)));
						AbstractClothing collar = Main.game.getPlayer().getClothingInSlot(InventorySlot.NECK);
						if(collar!=null) {
							Main.game.getPlayer().forceUnequipClothingIntoVoid(getOwner(), collar);
						}
					}
				};
				
			} else if(index==2) {
				if(!Main.game.getPlayer().hasPerkAnywhereInTree(Perk.CONVINCING_REQUESTS)) {
					return new Response("Seduce",
							UtilText.parse(getOwner(),
									"You aren't convincing enough at seduction to attempt to trick [npc.name] into taking your collar off..."
									+ "<br/>[style.italicsMinorBad(Requires the '"+Perk.CONVINCING_REQUESTS.getName(Main.game.getPlayer())+"' perk.)]"),
							null);
				}
				return new Response("Seduce",
						UtilText.parse(getOwner(),
								"Tell [npc.name] that you're desperate for sex in an attempt to trick [npc.herHim] into taking your collar off..."
								+ "<br/>[style.italicsMinorGood(Unlocked from having the '"+Perk.CONVINCING_REQUESTS.getName(Main.game.getPlayer())+"' perk.)]"),
						STOCKS_CALL_OUT_RELEASED) {
					@Override
					public void effects() {
						Main.game.getTextStartStringBuilder().append(UtilText.parseFromXMLFile("places/submission/ratWarrens/captive", "STOCKS_CALL_OUT_SEDUCE", getCharacters(true, false)));
						AbstractClothing collar = Main.game.getPlayer().getClothingInSlot(InventorySlot.NECK);
						if(collar!=null) {
							Main.game.getPlayer().forceUnequipClothingIntoVoid(getOwner(), collar);
						}
					}
				};
			}
			return null;
		}
	};
	
	public static final DialogueNode STOCKS_CALL_OUT_RELEASED = new DialogueNode("", "", true) {
		@Override
		public int getSecondsPassed() {
			return 5*60;
		}
		@Override
		public String getContent() {
			return "";
		}
		@Override
		public Response getResponse(int responseTab, int index) {
			if(index==1) {
				return new ResponseSex("Sex",
						UtilText.parse(getOwner(), "Have sex with [npc.name]..."),
						Util.newArrayListOfValues(Fetish.FETISH_SUBMISSIVE), null, Fetish.FETISH_SUBMISSIVE.getAssociatedCorruptionLevel(),
						null, null, null,
						true, false,
						new SMGeneric(
								Util.newArrayListOfValues(getOwner()),
								Util.newArrayListOfValues(Main.game.getPlayer()),
								null,
								Main.game.getPlayer().getCompanions()) {
							@Override
							public SexControl getSexControl(GameCharacter character) {
								if(character.isPlayer()) {
									return SexControl.ONGOING_PLUS_LIMITED_PENETRATIONS;
								}
								return super.getSexControl(character);
							}
						},
						STOCKS_RELEASED_AFTER_SEX,
						UtilText.parseFromXMLFile("places/submission/ratWarrens/captive", "STOCKS_CALL_OUT_RELEASED_SEX", getCharacters(true, false)));
				
			} else if(index==2) {//TODO Fight without companion
				return new ResponseCombat("Fight",
						UtilText.parse(getOwner(), "Now that you're free of your collar, you can finally attack [npc.name]!"),
						(NPC) getOwner(),
						Util.newHashMapOfValues(new Value<>(getOwner(), "[npc.speech(Yer gonna pay fer this!)] [npc.name] shouts as [npc.she] prepares to fight you."))) {
					@Override
					public void effects() {
						Main.game.getDialogueFlags().setFlag(DialogueFlagValue.ratWarrensCaptiveAttemptingEscape, true);
					}
				};
			}
			return null;
		}
	};
	
	public static final DialogueNode STOCKS_RELEASED_AFTER_SEX = new DialogueNode("", "", true) {
		@Override
		public int getSecondsPassed() {
			return 5*60;
		}
		@Override
		public String getContent() {
			return UtilText.parseFromXMLFile("places/submission/ratWarrens/captive", "STOCKS_RELEASED_AFTER_SEX", getCharacters(true, false));
		}
		@Override
		public Response getResponse(int responseTab, int index) {
			if(index==1) {
				return new Response("Locked up", "Let Murk lock you back into the stocks...", STOCKS_CALL_OUT_END_LOCKED_UP) {
					@Override
					public void effects() {
						Main.game.getTextStartStringBuilder().append(UtilText.parseFromXMLFile("places/submission/ratWarrens/captive", "STOCKS_RELEASED_AFTER_SEX_LOCKED_UP", getCharacters(true, false)));
						equipCollar(Main.game.getPlayer());
					}
				};
				
			} else if(index==2) {
				return new Response("Offer company", "Offer to sleep with Murk, which would give you the opportunity to sneak out and escape...", STOCKS_RELEASED_OFFER_COMPANY);
				
			}
			return null;
		}
	};
	
	public static final DialogueNode STOCKS_RELEASED_OFFER_COMPANY = new DialogueNode("", "", true) {
		@Override
		public int getSecondsPassed() {
			return 5*60;
		}
		@Override
		public String getContent() {
			return UtilText.parseFromXMLFile("places/submission/ratWarrens/captive", "STOCKS_RELEASED_OFFER_COMPANY", getCharacters(true, false));
		}
		@Override
		public Response getResponse(int responseTab, int index) {
			if(index==1) {
				return new Response("Slip away", "Use this opportunity to slip away and attempt to escape...", STOCKS_ESCAPING) {
					@Override
					public void effects() {
						Main.game.getTextStartStringBuilder().append(UtilText.parseFromXMLFile("places/submission/ratWarrens/captive", "STOCKS_RELEASED_OFFER_COMPANY_SLIP_AWAY", getCharacters(true, false)));
						restoreInventories(); //TODO remember to add description of essences
						Main.game.getDialogueFlags().setFlag(DialogueFlagValue.ratWarrensCaptiveAttemptingEscape, true);
					}
				};
				
			} else if(index==2) {
				return new Response("Stay", "Stay with Murk until it's time for you to be locked back into the stocks...", STOCKS_CALL_OUT_END_LOCKED_UP) {
					@Override
					public void effects() {
						Main.game.getTextStartStringBuilder().append(UtilText.parseFromXMLFile("places/submission/ratWarrens/captive", "STOCKS_RELEASED_OFFER_COMPANY_LOCKED_UP", getCharacters(true, false)));
						equipCollar(Main.game.getPlayer());
					}
				};
			}
			return null;
		}
	};
	
	public static final DialogueNode STOCKS_CALL_OUT_END_LOCKED_UP = new DialogueNode("", "", true) {
		@Override
		public int getSecondsPassed() {
			return STOCKS_SLEEP.getSecondsPassed(); // Advance to morning
		}
		@Override
		public String getContent() {
			return "";
		}
		@Override
		public Response getResponse(int responseTab, int index) {
			if(index==1) {
				return new Response("Sleep", "Fall asleep...", STOCKS_SLEEP);
			}
			return null;
		}
	};
	
	public static final DialogueNode STOCKS_ESCAPE_FIGHT_VICTORY = new DialogueNode("", "", true) {
		@Override
		public int getSecondsPassed() {
			return 2*60;
		}
		@Override
		public String getContent() {
			return UtilText.parseFromXMLFile("places/submission/ratWarrens/captive", "STOCKS_ESCAPE_FIGHT_VICTORY", getCharacters(true, false));
		}
		@Override
		public Response getResponse(int responseTab, int index) {
			if(index==1) {
				return new Response("Tunnels", "Head back through the tunnels and escape from the Rat Warrens.", STOCKS_ESCAPING) {
					@Override
					public void effects() {
						Main.game.getTextStartStringBuilder().append(UtilText.parseFromXMLFile("places/submission/ratWarrens/captive", "STOCKS_ESCAPE_FIGHT_VICTORY_ESCAPING", getCharacters(true, false)));
						restoreInventories(); //TODO remember to add description of essences
					}
				};
			}
			return null;
		}
	};
	
	public static final DialogueNode STOCKS_ESCAPE_FIGHT_DEFEAT = new DialogueNode("", "", true) {
		@Override
		public int getSecondsPassed() {
			return 2*60;
		}
		@Override
		public String getContent() {
			return UtilText.parseFromXMLFile("places/submission/ratWarrens/captive", "STOCKS_ESCAPE_FIGHT_DEFEAT", getCharacters(true, false));
		}
		@Override
		public Response getResponse(int responseTab, int index) {
			if(index==1) {
				return new Response("Collared", UtilText.parse(getOwner(), "[npc.Name] puts your collar back on..."), STOCKS_ESCAPE_FIGHT_DEFEAT_SEX) {
					@Override
					public void effects() {
						equipCollar(Main.game.getPlayer());
						calculatePlayerSexType();
					}
				};
			}
			return null;
		}
	};
	
	public static final DialogueNode STOCKS_ESCAPE_FIGHT_DEFEAT_SEX = new DialogueNode("", "", true) {
		@Override
		public int getSecondsPassed() {
			return 2*60;
		}
		@Override
		public String getContent() {
			if(playerSexType.getValue().equals(new SexType(SexParticipantType.NORMAL, SexAreaPenetration.PENIS, SexAreaOrifice.ANUS))) {
				return UtilText.parseFromXMLFile("places/submission/ratWarrens/captive", "DEFEAT_SEX_ANAL", getCharacters(true, false));
				
			} else if(playerSexType.getValue().equals(new SexType(SexParticipantType.NORMAL, SexAreaPenetration.PENIS, SexAreaOrifice.VAGINA))) {
				return UtilText.parseFromXMLFile("places/submission/ratWarrens/captive", "DEFEAT_SEX_VAGINAL", getCharacters(true, false));
				
			} else if(playerSexType.getValue().equals(new SexType(SexParticipantType.NORMAL, SexAreaPenetration.PENIS, SexAreaOrifice.MOUTH))) {
				return UtilText.parseFromXMLFile("places/submission/ratWarrens/captive", "DEFEAT_SEX_BLOWJOB", getCharacters(true, false));
				
			} else if(playerSexType.getValue().equals(new SexType(SexParticipantType.NORMAL, SexAreaOrifice.VAGINA, SexAreaPenetration.TONGUE))) {
				return UtilText.parseFromXMLFile("places/submission/ratWarrens/captive", "DEFEAT_SEX_CUNNILINGUS", getCharacters(true, false));
				
			} else if(playerSexType.getValue().equals(new SexType(SexParticipantType.NORMAL, SexAreaOrifice.ANUS, SexAreaPenetration.TONGUE))) {
				return UtilText.parseFromXMLFile("places/submission/ratWarrens/captive", "DEFEAT_SEX_ANILINGUS", getCharacters(true, false));
				
			} else if(playerSexType.getValue().equals(new SexType(SexParticipantType.NORMAL, SexAreaPenetration.TONGUE, SexAreaOrifice.ANUS))) {
				return UtilText.parseFromXMLFile("places/submission/ratWarrens/captive", "DEFEAT_SEX_RECEIVE_ANILINGUS", getCharacters(true, false));
				
			} else if(playerSexType.getValue().equals(new SexType(SexParticipantType.NORMAL, SexAreaPenetration.FINGER, SexAreaPenetration.PENIS))) {
				return UtilText.parseFromXMLFile("places/submission/ratWarrens/captive", "DEFEAT_SEX_RECEIVE_HANDJOB", getCharacters(true, false));
				
			} else if(playerSexType.getValue().equals(new SexType(SexParticipantType.NORMAL, SexAreaPenetration.FINGER, SexAreaOrifice.VAGINA))) {
				return UtilText.parseFromXMLFile("places/submission/ratWarrens/captive", "DEFEAT_SEX_RECEIVE_FINGERING", getCharacters(true, false));
				
			} else if(playerSexType.getValue().equals(new SexType(SexParticipantType.NORMAL, SexAreaPenetration.TONGUE, SexAreaOrifice.VAGINA))) {
				return UtilText.parseFromXMLFile("places/submission/ratWarrens/captive", "DEFEAT_SEX_RECEIVE_CUNNILINGUS", getCharacters(true, false));
			}
			return UtilText.parseFromXMLFile("places/submission/ratWarrens/captive", "DEFEAT_SEX", getCharacters(true, false));
		}
		@Override
		public Response getResponse(int responseTab, int index) {
			if(index==1) {
				String introDescriptionPath = "BEG_FOR_SEX";
				if(playerSexType.getValue().equals(new SexType(SexParticipantType.NORMAL, SexAreaPenetration.PENIS, SexAreaOrifice.ANUS))) {
					introDescriptionPath = "BEG_FOR_SEX_ANAL";
					
				} else if(playerSexType.getValue().equals(new SexType(SexParticipantType.NORMAL, SexAreaPenetration.PENIS, SexAreaOrifice.VAGINA))) {
					introDescriptionPath = "BEG_FOR_SEX_VAGINAL";
					
				} else if(playerSexType.getValue().equals(new SexType(SexParticipantType.NORMAL, SexAreaPenetration.PENIS, SexAreaOrifice.MOUTH))) {
					introDescriptionPath = "BEG_FOR_SEX_BLOWJOB";
					
				} else if(playerSexType.getValue().equals(new SexType(SexParticipantType.NORMAL, SexAreaOrifice.ANUS, SexAreaPenetration.TONGUE))) {
					introDescriptionPath = "BEG_FOR_SEX_ANILINGUS";
					
				} else if(playerSexType.getValue().equals(new SexType(SexParticipantType.NORMAL, SexAreaPenetration.TONGUE, SexAreaOrifice.ANUS))) {
					introDescriptionPath = "BEG_FOR_SEX_RECEIVE_ANILINGUS";
					
				} else if(playerSexType.getValue().equals(new SexType(SexParticipantType.NORMAL, SexAreaOrifice.MOUTH, SexAreaPenetration.PENIS))) {
					introDescriptionPath = "BEG_FOR_SEX_RECEIVE_BLOWJOB";
					
				} else if(playerSexType.getValue().equals(new SexType(SexParticipantType.NORMAL, SexAreaPenetration.FINGER, SexAreaPenetration.PENIS))) {
					introDescriptionPath = "BEG_FOR_SEX_RECEIVE_HANDJOB";
					
				} else if(playerSexType.getValue().equals(new SexType(SexParticipantType.NORMAL, SexAreaPenetration.FINGER, SexAreaOrifice.VAGINA))) {
					introDescriptionPath = "BEG_FOR_SEX_RECEIVE_FINGERING";
					
				} else if(playerSexType.getValue().equals(new SexType(SexParticipantType.NORMAL, SexAreaPenetration.TONGUE, SexAreaOrifice.VAGINA))) {
					introDescriptionPath = "BEG_FOR_SEX_RECEIVE_CUNNILINGUS";
				}
				return getPlayerOwnerSexResponse("Submit", UtilText.parse(getOwner(), "Do as [npc.name] says and tell [npc.herHim] that you'll be [npc.her] good milker!"), STOCKS_AFTER_DEFEAT_SEX, introDescriptionPath);
			}
			return null;
		}
	};
	
	public static final DialogueNode STOCKS_AFTER_DEFEAT_SEX = new DialogueNode("Finished", "", true) {
		@Override
		public String getDescription() {
			return UtilText.parse(getOwner(), "[npc.NameHasFull] finished with you for now...");
		}
		@Override
		public int getSecondsPassed() {
			return 10*60;
		}
		@Override
		public String getContent() {
			return UtilText.parseFromXMLFile("places/submission/ratWarrens/captive", "STOCKS_AFTER_DEFEAT_SEX", getCharacters(true, false));
		}
		@Override
		public Response getResponse(int responseTab, int index) {
			if(index==1) {
				return new Response("Sleep", "Completely exhausted from your fight and the subsequent sex, you soon find yourself falling asleep...", STOCKS_SLEEP);
			}
			return null;
		}
	};
	
	public static final DialogueNode STOCKS_ESCAPING = new DialogueNode("", "", true) {
		@Override
		public int getSecondsPassed() {
			return 10*60;
		}
		@Override
		public String getContent() {
			return "";
		}
		@Override
		public Response getResponse(int responseTab, int index) {
			if(index==1) {
				return new Response("Follow", "Follow Shadow and Silence as they lead you out of the Rat Warrens.", ESCAPING) {
					@Override
					public void effects() {
						RatWarrensCaptiveDialogue.restoreInventories();
						Main.game.getDialogueFlags().setFlag(DialogueFlagValue.playerCaptive, false);
						Main.game.getPlayer().setLocation(WorldType.RAT_WARRENS, PlaceType.RAT_WARRENS_ENTRANCE);
						Main.game.getNpc(Shadow.class).setLocation(WorldType.RAT_WARRENS, PlaceType.RAT_WARRENS_ENTRANCE);
						Main.game.getNpc(Silence.class).setLocation(WorldType.RAT_WARRENS, PlaceType.RAT_WARRENS_ENTRANCE);
					}
				};
			}
			return null;
		}
	};

	public static final DialogueNode ESCAPING = new DialogueNode("", "", true) {
		@Override
		public int getSecondsPassed() {
			return 10*60;
		}
		@Override
		public String getContent() {
			return UtilText.parseFromXMLFile("places/submission/ratWarrens/captive", "ESCAPING");
		}
		@Override
		public Response getResponse(int responseTab, int index) {
			if(index==1) {
				return new Response("Escape", "Follow Shadow through the tunnels.", RatWarrensDialogue.POST_CAPTIVITY_SWORD_RAID) {
					@Override
					public void effects() {
						Main.game.getPlayer().setLocation(WorldType.SUBMISSION, PlaceType.SUBMISSION_RAT_WARREN);
						Main.game.getNpc(Shadow.class).setLocation(WorldType.SUBMISSION, PlaceType.SUBMISSION_RAT_WARREN);
						Main.game.getNpc(Silence.class).setLocation(WorldType.SUBMISSION, PlaceType.SUBMISSION_RAT_WARREN);
					}
				};
			}
			return null;
		}
	};
	
}