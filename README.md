# Exploding Chickens web implementation (server)

Exploding Chickens is a spin-off of the popular game 'Exploding Kittens'. In this case the protagonists are lovely chickens (but not so lovely, they can explode!). We created this unique implementation to connect different people through the web implementation of this game, before this alternative it was only possible to play it when they were co-located.

This project provides an innovative user interface as well as other features that enhance mutual collaboration between the players while having a good user experience, such as, the ability to send friend requests, a chat to discuss strategies, high quality graphics, different fun play modes, among others.

## Technologies

The server was implemented using the following technologies
-   Java
-   Spring boot
-   Gradle
-   Web socket communication
-   Database to enable persistance
-   API calls (email and cards management)
-   Google cloud for deployment
-   Git for version control and collaboration among developers
-   Github to follow agile practices
-   Mockito and JUnit for testing

## Main components
-  GameController.java (https://github.com/sopra-fs24-group-17/server/blob/develop/src/main/java/ch/uzh/ifi/hase/soprafs24/controller/GameController.java) : The game controller is where all the requests from the client involving the game are receive, including, but not limited to starting a game, ending a game, and joining users to the game.
-  GameEngineController.java (https://github.com/sopra-fs24-group-17/server/blob/develop/src/main/java/ch/uzh/ifi/hase/soprafs24/controller/GameEngineController.java) : The game engine controller is where all the requests from the client regarding the development of the game are received. For example, what happens when a user plays a cards or draws a card.   
-  GameDeckService.java (https://github.com/sopra-fs24-group-17/server/blob/develop/src/main/java/ch/uzh/ifi/hase/soprafs24/service/GameDeckService.java) : This service controls the interaction between our application and the Deck API. Though this component we make requests to the API and receive the responses, which play a fundamental role in our game.
-  GameEngineService.java (https://github.com/sopra-fs24-group-17/server/blob/develop/src/main/java/ch/uzh/ifi/hase/soprafs24/service/GameEngineService.java) : The game engine service defines the logic that drives the execution of the game. For example, managing who is the active user and avoiding other usesrs can play when it is not their turn, as well as what should happen when a user play a specific card, for example 'Shuffle'. 
-  WebSocketService.java (https://github.com/sopra-fs24-group-17/server/blob/develop/src/main/java/ch/uzh/ifi/hase/soprafs24/service/WebSocketService.java) : The WebSocket service defines what should be sent to the client when specific events take place. This is a fundamental part since without it, it would be impossible to have a smooth user experience.


## Launch and Deployment
#### Cloning the repository:
`git clone https://github.com/sopra-fs24-group-17/server.git`\
`cd server`

#### Locally
- Build : `./gradlew build`
- Run : `./gradlew bootRun`
- Test: `./gradlew test`

You can verify that the server is running by visiting `localhost:8080` in your browser.
You can access the local hosted application (client) by visiting `localhost:3000`

#### Cloud service
The application is hosted in google cloud service. 
To make a new deployment (release) it is necessary to merge the release branch with our main branch, this will automatically start the deployment in google cloud. It is important to also activate the database in google cloud (Ellaborate more about how to do this - if needed). 

Finally, it is a good practice to mark a new release with a tag, for example 'M4' represents the release at Milestone 4.


### Roadmap (next steps)
- On game store : Thinking from a buisiness perspective and a way to generate revenue an online store where users can buy upgrades to the game, customizable cards, among others would be a great addition to make this project self-sustainable.
- Enhance game features : To make a game fun and reach more people is necessary to innovate. It would be desired to incorporate new cards with new effects in the game as well as having more game modes that would cautivate all the players. For extending the game it would be necessary to develop the implementation of new cards or new game modes both for the client and the server, but the existing implementations can be followed as how-to guideline.

## Authors
- Kevin Bründler (random9ness)
- Liam Tessendorf (liamti5)
- Liam Kane (ljkane)
- Panagiotis Patsias (PanagiotisPatsias)
- Jorge Ortiz (jorgeortizv)

### License
This project is licensed under the MIT License. For more details, see the LICENSE file.



