# Distributed Tic Tac Toe System

I have designed and implemented a distributed Tic Tac Toe system utilising
Remote Method Invocation for both client and server components. The system allows for
real-time gameplay, enabling multiple players to compete as well as chat with their opponents.
For the user interface, I have used Java Swing on the client side.

To enhance the gameplay experience, I have integrated a client-side timeout feature, where
players are given a 20 second timeframe to make their move, failing to do so triggers the system
to make a random move on their behalf. Another addition is the system's fault tolerance to
network disconnection, so when a player loses connection, the game pauses for 30 seconds,
offering them a time period to reconnect and resume the match.

Furthermore, I have introduced a player ranking system. Players are evaluated based on their
performance across matches: they gain 5 points for every win, lose 5 for a defeat, and earn 2
points for a draw.

## System Components:

#### Client
##### TicTacToeClient

The TicTacToeClient is the backbone of the client-side of the game, playing a key role in
managing the game logic, player interaction and communication with the server.

**Attributes**
● Player's and opponent's name, rank, character (X or O).

● Current players turn.

● References to the server for remote method invocation, and the GUI for visual
representation

**Functionality**
● Initialisation of player's data.

● Communicating with the server for various actions like joining a game, making a move,
quitting, sending/receiving messages, and sending heartbeat timestamps.

● Handling game state, including keeping track of whose turn it is.

● Calling upon GUI methods to update the visual representation based on game status.

##### TicTacToeGUI
The TicTacToeGUI provides a graphical user interface(GUI) for players to interact with the
game, including playing the game and chatting with the opponent.

**Game Interface:**

● A 3x3 grid of buttons representing the Tic-Tac-Toe board, which players are able to click
to make moves.

● A Game Info label displaying the current status of the game, which player’s turn it is, the
game winner, or draw game in the case of a draw match.

● A countdown timer showing the time left for the current player to make a move. When
the timer reaches zero, a random move is made on the player's behalf.

● A quit button, which allows players to quit the game.

**Chat System:
● Chat Area where players can see messages from their opponent.
● Chat Input field where players can input their messages.
● Send Button to send the typed message to the opponent.
##### Server Communication:
Both the TicTacToeClient and the TicTacToeGUI rely heavily on server communication,
facilitated by the RMI (Remote Method Invocation) framework, to manage the game state and
players actions. The client interacts with the server to:
● Join or quit a game.
● Make a move on the board.
● Retrieve the current state of the board.
● Send or receive chat messages.
● Handle game pause and resume functionalities.

