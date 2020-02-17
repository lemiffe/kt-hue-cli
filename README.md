# Kotlin Hue Lights CLI

## Building and running it locally:
- Install JDK 1.8 at least
- ./gradlew build
- ./gradlew run

## Installation (from release tar):
- tar -xvf kt-lights.tar
- You'll find the executable script in `kt-lights/bin`

## Example usage:
- `./kt-lights --help`
- `./kt-lights --setup`
- `./kt-lights --rooms`
- `./kt-lights --lights "Team A"`
- `./kt-lights --turnOn="Team A"`
- `./kt-lights --turnOn="Team A" --color=BLUE`
- `./kt-lights --turnOn="Team A" --color=RED`
- `./kt-lights --turnOff="Team A"`

## Colors:
```
WHITE
RED
GREEN
BLUE
YELLOW
ORANGE
MAGENTA
PINK
CYAN
```
