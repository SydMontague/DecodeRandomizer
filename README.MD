# Digimon World Re:Digitize Decode Randomizer

This software allows randomizing several aspects of the 2013 released 3DS game "Digimon World Re:Digitize Decode".

# Usage

## Prerequisites 
Before using this software make sure you

* have Java 11 or higher installed
  * you can download it from [AdoptOpenJDK](https://adoptopenjdk.net/)
* have a dumped ROM of the game in .CCI/.3DS or .CIA format
  * the ROM can have the English translation patch applied
  * other modifications can generally be compatible as well, but success may vary
* have enough empty disk space for all files created during the randomization
  * 5-10 GiB on the drive you installed the randomizer on should suffice

## Setup

Download the appropriate version for your OS from the [releases tab](https://github.com/SydMontague/DecodeRandomizer/releases) and unpack the downloaded .zip archive in the location you want to install the randomizer at.

Run the `Decode-Randomizer.jar`, press the "Select Base ROM" button and navigate to ROM you want to use as base for the randomizer.
After the ROM has been successfully unpacked you'll be presented with the randomization options.

# Current Features

## Randomization

* Digimon Properties
  * Attribute (Vaccine, Virus, Data)
  * Sleep Schedule
  * Favorite Food
  * Base Weight
  * Combat Speed
  * Training Type
  * Size (joke option, might break things)
* Skills and Finisher
  * MP Costs
  * Power
  * Status
  * Status Chance 
  * Cooldown
  * Learn Rate
* Evolutions
  * Requirements
  * Paths
  * Statsgains
* Enemies NPCs
  * Digimon
  * Stats
  * Skills
  * Drops
* Player character and starter Digimon
  * You can also use it to set it to a certain character or Digimon
* Other
  * Type Alignments (i.e. X is strong against Y)
  * Treasure Hunt items
  * map item spawns
 
## Patches
* Increased view distance
* Increased movement speed
* Increase skill learning chance from brains training
* Start with 5 MP Discs instead of Meat
  * to help with running out of MP in the first fight
* add 19 previously unobtainable Digimon to the roster

## Known Issues / Caveats
Certain seed/settings combinations might be unplayable, for example if your starting move is too weak/expensive or if your Digimon is scaled too large to be able to hit.
Use another seed or disable the respective settings to solve this.

# Planned Features
* more fine control for everything

# Contact
* Discord: SydMontague#8056, or in either the [Digimon Modding Community](https://discord.gg/cb5AuxU6su) or [Digimon Discord Community](https://discord.gg/0VODO3ww0zghqOCO)
* directly on GitHub
* E-Mail: sydmontague@web.de
* Reddit: [/u/Sydmontague](https://reddit.com/u/sydmontague)