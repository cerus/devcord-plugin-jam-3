# Battle Royale

This plugin was made during the 3rd DevCord Pluginjam. The theme was "It's a feature, not a bug". Because we only had 48 hours to work on this the
code is very messy and scuffed. Our only
priority was to make it work and not to make it maintainable.

## The game

This plugin is a Battle Royale gamemode. At the beginning of each round players will spawn in the middle of a map with an elytra equipped. They can
then fly whereever they want and loot chests that are scattered across the map. The goal is to eliminate other players and be the last player
standing.
The world is filled with goofy looking mobs and weird ~~bugs~~ features which makes the experience unique and challenging.

## ~~Bugs~~ Features

- Sheeps behave similar to ignited creepers when shorn
- Endermen steal your items when you anger them
- Lava and water switch randomly
- Items don't have gravity
- The sun has a strong gravitational effect on entities
- Mobs look bigger the further you are away from them
- Portals teleport you to a random player
- Shooting players with a bow will display the Minecraft credits
- Getting shot with an arrow will display the Minecraft credits
- Getting shot with an arrow will cause you to look like a hedgehog

## Actual features

- Players spawn with a map in their offhand which displays the entire game area (including other players and their death locations)
- The border shrinks every few minutes
- Once the border has shrunken to its final level a Sudden Death mode is activated

## Building

1. `git clone https://github.com/cerus/devcord-plugin-jam-3 battleroyale`
2. `cd battleroyale`
3. `./gradlew clean build`
4. Final jar is at `build/libs/gamejam-1.0.0.jar`

## Server setup

All you need to do is copy the plugin into your servers plugins folder. The plugin will use the default world (called `world`). Warning: The plugin
deletes and regenerates the world on startup.

### Made with <3 by

[Lukas](https://github.com/lus) and [Max](https://github.com/cerus)