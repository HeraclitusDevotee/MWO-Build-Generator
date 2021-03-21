# MWO Build Generator

## A file-based combinatorial generator and rating system for 'mech loadouts. (ALPHA)

Given a set of parameters such as range, speed, duration of engagement, and duration between engagements; loadouts optimizing damage output are generated for each 'mech. The loadouts are then compared between 'mechs, producing a list of ratings.

Example loadouts section for a 'mech (DPE is short for damage per engagement):

    TIMBER WOLF S 375 at 87.57kph

    XL engine

        FER armor

            DPE 56.5 hot DPS 12.43 fire duration 2.95 cold DPS 7.51

            added heat sinks 0 capacity 55.88 dissipation 3.63

            post-engage remaining capacity 18.14 engage interval 10.4

            mech slots 19 loadout slots 10 stripping left arm

            1 shots from 1 C-UAC/10 ammo 1.5 cooldown 3.38 duration 0.19 HPD 0.32

            1 shots from 1 C-ERPPC cooldown 6.33 HPD 0.96

            2 shots from 2 C-ERLL cooldown 3.62 duration 1.22 HPD 1.01


Example of a ratings list:

    ARCHER T XL engine STD armor 99.0 DPE rating:89.40981:

    ARCHER 2R XL engine STD armor 99.0 DPE rating:89.21353:

    ORION M XL engine LHT armor 93.0 DPE rating:86.84021:

    ORION M XL engine STD armor 93.0 DPE rating:86.46379:

    THANATOS HA XL engine LHT armor 94.75 DPE rating:86.32587:

    ARCHER 5S XL engine STD armor 94.0 DPE rating:85.866425:

    WARHAMMER 7S XL engine STD armor 95.51 DPE rating:85.47827:


WARNING: This program will use ALL of your CPU and a lot of memory. This is not a problem for quick calculations, but using parameters that cast a very wide net can lead up to an hour of processing. Ensure you have adequate cooling!


What it Takes Into Account:

    quirks

    skills

    engine types

    armor types

    for each loadout, if the arms are not needed they are stripped and the weight is used for additional heat sinks

    MASC bonus is averaged and applied to allow 'mechs to take lighter engines while still reaching the desired speed.


What it Doesn't Take Into Account:

    omnipod swapping (custom omnimechs can be added to the 'mech data file (with two examples already added))

    locational weapon placement - weapons are added based on the sum of hardpoints and available slots for those hardpoints, they are not actually placed into arms,torsos etc., so invalid loadouts may be produced

    standard structure - a combo pass is not done for standard structure (although 'mechs with fixed standard structure are calculated appropriately), endo steel is better 99% of the time so why nearly double processing time for it?

    locking missile weapons - locking missile weapons have far too many x-factors (AMS,ECM,BAP,ARTEMIS,TAG,NARC...). Including those would just lead to less useful results.

    shot velocity

    fixed slot and external heat sink locations (e.g. direwolf) on omnimechs

    the data is up to date with 1.4.239.0, but may not be 100% accurate (such as which 'mechs have hand actuators on which arms), as much of it was manually entered.

    bugs


Usage:

Open the configuration file and set your parameters, run the .bat file and wait for processing to complete. A folder structure representing your parameters will be created, where you can view a loadouts file and a ratings file. Use the ratings file to copy the name of a 'mech and find (ctrl+f) it in the loadouts file.
