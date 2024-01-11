# *Introducing Perudo game*
This school-project aims to transform the traditional **Perudo** game into a modern, simple and engaging digital online product.

Perudo, also known as Liar's Dice, is a classic dice game that combines strategy, bluffing, and a bit of luck.
- Italian [Perudo rules.](https://drive.google.com/file/d/1VzQnszzUQIVI9YLBmQGH977Ybmoo1TDx/view?usp=drivesdk)

## *How to properly start a server*

Before launching the server, ensure that a local **MySQL Database** is running.

We recommend using [XAMPP](https://www.apachefriends.org/). Once installed, open **XAMPP**, and start both **Apache** and **MySQL**.

To initiate the server, open the game and choose **Run as Server**. Once the necessary information is provided, the server will run.

## *Technology Stack*

The game has been developed using **Java SE 21** and utilizes **MySQL** for data storage.

### *Custom interface*

- Our project features a bespoke **Java Swing interface** designed to maintain the classic console aesthetic.
- This unique design not only retains the charm of the traditional look but also incorporates modern features such as **options selection**, color schemes, and more.

### *Storage*

- The project boasts a **MySQL-based** storage system for the server, ensuring robust data storage for player and lobby information. This implementation provides **exceptional resilience**, even in cases of server crashes.

- Lobbies data is persistently stored for **up to an hour** from their last usage. In the event of a server crash, lobbies will be reloaded and stay up for a **10-minute window** of non-utilization.
  
- Lobby passwords undergo **Argon2 hashing** before storage, ensuring secure and robust encryption.

- The project also includes client data savings through a **Json** file. This file stores essential information such as settings, **lobby authorization token**, and frequently asked inputs for enhanced user convenience.

### *Networking & Security*

- The project implements a robust networking system utilizing **RSA encryption** and **Sha256 with RSA Signature** for secure data transfers.

- The server has the capability to listen to an **indefinite number** of clients and concurrently handle **multiple requests** at the time from each client.

- Lobbies are equipped with a secure and unique **authentication token** generated using **UUIDs**. This token allows players to rejoin the lobby in case of connection loss during gameplay, while also preventing others from joining with the same username.
