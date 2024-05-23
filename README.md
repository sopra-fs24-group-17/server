# Exploding Chickens web implementation (server)

Exploding Chickens is a spin-off of the popular game 'Exploding Kittens'. In this version, the protagonists are lovely chickens (but not so lovely, they can explode!). We created this unique implementation to conect people through a web implementation of this game, as previously it was only possible to play  when players were co-located.

This project offers an innovative user interface and features that enhance collaboration between players while providing a good user experience. These features include the ability to send friend requests, a chat to discuss strategies, high-quality graphics, and various fun play modes.

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
-  [GameController.java] (https://github.com/sopra-fs24-group-17/server/blob/develop/src/main/java/ch/uzh/ifi/hase/soprafs24/controller/GameController.java) : Handles client requests related to the game, such as starting, ending, and joining games. 
-  [GameEngineController.java] (https://github.com/sopra-fs24-group-17/server/blob/develop/src/main/java/ch/uzh/ifi/hase/soprafs24/controller/GameEngineController.java) :  Manages requests concerning the game mechanics, including playing and drawing cards.  
-  [GameDeckService.java] (https://github.com/sopra-fs24-group-17/server/blob/develop/src/main/java/ch/uzh/ifi/hase/soprafs24/service/GameDeckService.java) : Interfaces with the [Deck of Cards API](https://www.deckofcardsapi.com/) : Interfaces with the Deck API to request and receive responses that are crucial for game functionality.
-  [GameEngineService.java] (https://github.com/sopra-fs24-group-17/server/blob/develop/src/main/java/ch/uzh/ifi/hase/soprafs24/service/GameEngineService.java) :  Defines the logic driving game execution, including turn management and card effect processing.
-  [WebSocketService.java] (https://github.com/sopra-fs24-group-17/server/blob/develop/src/main/java/ch/uzh/ifi/hase/soprafs24/service/WebSocketService.java) : Manages WebSocket communication to ensure a smooth user experience by sending updates to clients.


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

NOTE: The client repository can be found [here](https://github.com/sopra-fs24-group-17/client)

#### Cloud service
The application is hosted in google cloud service. 
To make a new deployment (release) it is necessary to merge the release branch with our main branch, this will automatically start the deployment in google cloud. It is important to also activate the database in google cloud (Ellaborate more about how to do this - if needed). 

Finally, it is a good practice to mark a new release with a tag, for example 'M4' represents the release at Milestone 4.


### Roadmap (next steps)
- On game store : Thinking from a buisiness perspective and a way to generate revenue an online store where users can buy upgrades to the game, customizable cards, among others would be a great addition to make this project self-sustainable.
- Enhance game features : To make a game fun and reach more people is necessary to innovate. It would be desired to incorporate new cards with new effects in the game as well as having more game modes that would cautivate all the players. For extending the game it would be necessary to develop the implementation of new cards or new game modes both for the client and the server, but the existing implementations can be followed as how-to guideline.

## Authors
- Kevin Bründler [random9ness](https://github.com/random9ness)
- Liam Tessendorf [liamti5](https://github.com/liamti5)
- Liam Kane [ljkane](https://github.com/ljkane)
- Panagiotis Patsias [PanagiotisPatsias](https://github.com/PanagiotisPatsias)
- Jorge Ortiz [jorgeortizv](https://github.com/jorgeortizv)

### License
This project is licensed under the MIT License. For more details, see the [LICENSE](https://github.com/sopra-fs24-group-17/server/blob/main/LICENSE.txt) file.



