
## Table of Content
- [Introduction](#introduction) 
- [Built With](#built-with)
- [Main Components](#main-components)
- [Launch and Deployment](#launch-and-deployment)
- [Roadmap (next steps)](#roadmap-next-steps)
- [Authors](#authors)
- [Acknowledgements](#acknowledgments)
- [License](#license)

## Introduction

Exploding Chickens is a spin-off of the popular game 'Exploding Kittens'. In this version, the protagonists are lovely chickens (but be careful, they can explode!). We created this unique implementation to connect people through a web implementation of this game, as previously it was only possible to play when players were co-located.

This project offers an innovative user interface and features that enhance collaboration between players while providing a good user experience. These features include the ability to send friend requests, a chat to discuss strategies, high-quality graphics, and various fun play modes.

## Built With
* [React](https://react.dev/) - Front-end JavaScript library
* [Spring](https://spring.io/projects/spring-framework) - Java Back-end framework
* [Gradle](https://gradle.org/) - Build automation tool
* [STOMP](https://stomp-js.github.io/stomp-websocket/) - Bidirectional real time communication over websockets
* [DeckOfCardsAPI](https://www.deckofcardsapi.com/) - External API to simulate card decks
* [PerspectiveAPI](https://www.perspectiveapi.com/) - External API for content moderation (checking usernames and emails for toxicity)
* [GmailAPI](https://developers.google.com/gmail/api/reference/rest) - External API for email interaction for password reset
* [PostgreSQL](https://www.postgresql.org/) - Persistent Database (through instance of CloudSQL)

## Main components
[GameController](https://github.com/sopra-fs24-group-17/server/blob/develop/src/main/java/ch/uzh/ifi/hase/soprafs24/controller/GameController.java)
Handles client requests related to creation/joining/leaving of game sessions. 

[GameEngineController](https://github.com/sopra-fs24-group-17/server/blob/develop/src/main/java/ch/uzh/ifi/hase/soprafs24/controller/GameEngineController.java)
Manges requests from game sessions surrounding card moves, move terminations, etc.  

[GameDeckService](https://github.com/sopra-fs24-group-17/server/blob/develop/src/main/java/ch/uzh/ifi/hase/soprafs24/service/GameDeckService.java)
Interacts with the [Deck of Cards API](https://www.deckofcardsapi.com/) to simulate card decks for the game sessions.

[GameEngineService](https://github.com/sopra-fs24-group-17/server/blob/develop/src/main/java/ch/uzh/ifi/hase/soprafs24/service/GameEngineService.java) 
This is the core of our application as it hosts the logic of the game.

[WebSocketService](https://github.com/sopra-fs24-group-17/server/blob/develop/src/main/java/ch/uzh/ifi/hase/soprafs24/service/WebSocketService.java)
Manages bidirectional, real-time communication between client and server.


## Launch and Deployment
#### Cloning the repository:
`git clone https://github.com/sopra-fs24-group-17/server.git`\
`cd server`

#### Locally
- Build : `./gradlew build`
- Run : `./gradlew bootRun`
- Test: `./gradlew test`

You can verify that the server is running by visiting `localhost:8080` in your browser. By default the development profile is active, utilizing an in-memory H2 database.
You can access the local hosted application (client) by visiting `localhost:3000`

NOTE: The client repository can be found [here](https://github.com/sopra-fs24-group-17/client)

#### Cloud service
- through GitHub workflows, the main branch is automatically deployed onto Google Cloud's App Engine
- during deployment, the in-memory H2 database is migrated to a CloudSQL instance of PostgreSQL
  - (Note: if changes to the database schema are done, the database must be restarted upon deployment)
- during deployment, credentials for Google App Engine, PostgreSQL and the Gmail API are replaced with GitHub secrets


## Roadmap (next steps)
Below is an outline of suggested features that developers who want to contribute to our project could use as a starting point:
- new game modes, e.g. higher probability of explosions and new custom game cards
- implementation of a mobile application
- an in game store to allow customization of user profiles and avatars

## Authors
* **Liam Tessendorf** - (*Frontend*) - [liamti5](https://github.com/liamti5)
* **Liam Kane** - (*Frontend*) - [ljkane](https://github.com/ljkane)
* **Panagiotis Patsias** - (*Frontend*) - [PanagiotisPatsias](https://github.com/PanagiotisPatsias)
* **Jorge Ortiz** - (*Backend*) - [jorgeortizv](https://github.com/jorgeortizv)
* **Kevin Bründler** - (*Backend*) - [random9ness](https://github.com/random9ness)

## Acknowledgments
We would like to thank the professor and tutors of the Software Engineering Lab course from the Univeristy of Zurich.
A special thanks to our tutor [feji08](https://github.com/feji08) for the weekly meetings and continuous support.

## License
This project is licensed under the MIT License. For more details, see the [LICENSE](https://github.com/sopra-fs24-group-17/server/blob/main/LICENSE.txt) file.
