# Real-Time-e-Commerce-Broker-System

INTRODUCTION :

Real Time e-Commerce Broker System facilitates anonymous online purchase process between a Client and an e-Commerce Website.
It consists of three components namely Client, Broker and Web servers. The main goal of this project is to ensure the simulation of the purchase of a product in a secure way across the insecure network. Inorder to achieve this goal, features like Authentication, Message Integrity, Privacy, Anonymity and  Non- repudiation has to be implemented.

The Source code contains four different folders imitating the following -
1. Broker machine - One which facilitates a secure purchase of online item between the client and the web server
2. Client machine  - One who purchases the online item
3. Webserver1 machine - Acts like a real time web server (imitation of amazon.com) and allows client to purchase the online item available on it's site
4. Webserver2 machine - Another webserver serving the same purpose as Webserver1 (imitation of ebay.com).

SYSTEM COMPONENTS AND ITS IMPLEMENTATION :

Implementation Language: JAVA
Following are the files present on each of the above machines -
1)	AES.java – Encryption and Decryption using AES protocol.
2)	ecommerce_client.java, ecommerce_broker.java, ecommerce_webserver.java – Main class for implementing client, broker and web server components respectively.
    Note - Client machine has ecommerce_client.java. Broker machine has ecommerce_broker.java. Webserver machine has ecommerce_webserver.java. 
3)	Message.java - Class for implementing Message type. A Message object contains sender type , message type, cipher text and  message mac in it
4)	MiscellaneousWork.java – Class for reading configuration and password files and logging of the messages.
5)	RSA.java – Class for encrypting and decrypting using the public and private key between client and web server.
6)	Receiver.java – Class for handling the receiver thread.
7)	SyncMsgs.java – Class for maintaining synchronization among the threads.
8)  logfile - Contains the program generated logs.

Hence Each machine at a time runs two threads in parallel - Broker Thread/Client Thread/Webserver Thread and Receiver Thread

SYSTEM DESCRIPTION :

The system will ensure the following rules:
1. Client can browse to different e-Commerce site through Broker System.
2. Client’s profile as well as location is hidden from the e-Commerce site.
3. Broker System knows the Client and e-Commerce site but does not know about the purchased e-Product (music, movie or Application) or its contents.
4. Broker System pays for the e-Product to the e-Commerce site.
5. Broker System has sufficient information about a purchase transaction (but not what is purchased) by a Client which can help to protect against non-repudiation.

GOAL OF THE PROJECT:

1)	AUTHENTICATION
Each of the components should be able to authenticate each other. Following is a description of how the authentication occurs between each pair of components of the system -
Client and Broker – Long Time Shared key
Broker and Web server – Long Time Shared key
Client and Web server – Public and Private Key.

The “AES.Java” file imports the following classes 
1)	Key Generator – Class used in generation of keys.
2)	SecretKey – Class used in the generation of Session Key. The method “generate_session_key()” is used to generate a key of type SecretKey.
3)	SecretKeySpec – Class used for encryption and decryption of the messages

The LongTermShared keys and the Public key information are stored in the Password files for the individual components. The session key remains same throughout the compilation of the program.

2)  MESSAGE INTEGRITY / CONSISTENCY

Integrity of the messages exchanged between different components through the insecure network should be maintained.
This can be achieved by appending MAC ( Message Authentication Code )  to each message send over the network. The “generate_mac()”in  AES.java, does this by calculating the MAC of the message formatted as follows:
“Sender | Msg_type | Msg “ This method will return a byte[] array, which will be verified in the receiver side of the message.
A check_mac() method will be used to verify the MAC message from the sender side. Using the MAC method, we can ensure the message integrity has been taken care. 

3)  PRIVACY AND CONFIDENTIALITY 
Following Mechanisms has been used inorder to ensure confidentiality between communications of different components. 

1)	Encryption and Decryption
The key size used is 128 bits. Encryption and Decryption is achieved by the following methods: 
a)	encrypt_using_ltsk() – encryption done using the longterm shared key between the components
b)	encypt_using _sessionKey  - encryption done using the session key shared between the components
c)	decrypt_using_ltsk() – decryption done using the longterm shared key between the components
d)	decrypt_using _sessionKey()  - decryption done using the session key shared between the components
e)	send_encrypted_session_key()  -  encrypting the session keys during the initial session key exchange
f)	decrypt_session_key() – decrypting the session key during the initial exchange.

2)	Challenge
The Components will generate a challenge, which is an encrypted Random Number that will be sent along with the message. The class used is Random Integer and the method used is random().

4)	 ANONYMITY
Client’s Identity will be not known to the Web server. This is achieved by using Public and Private Key generated using RSA.
The client will know the public key of the web server. It will generate a session key with the web server through the broker. The message can only be decrypted with web server’s private key.
The methods used for the above actions are:
1)	encryptData() – used for the encryption of the messages using RSA algorithm.
2)	decryptData() – used for the decryption of the messages using RSA algorithm
3)	readPublicKeyFromFile() – reading the public key from the configuration file stored in the path of the program execution
4)	readPrivateKeyFromFile() – reading the private key from the configuration file stored in the path of the program execution


5)  NON-REPUDIATION
Using this feature a Client cannot deny the fact that it had purchased the item from a Webserver via the Broker. Hence it secures the Broker. 
This feature is achieved by using Digital Signature. Once the Client finishes it's purchase orders, Broker needs to ask Client's signature on the purchased document. The purchased document contains client ID, unique purchase ID generated by Broker, purchase date and webserver name.  
The methods used  to de the same in the code are:
Sign_message() -  used by the client to sign the message and send it to the broker. The broker will store the Client’s name, timestamp of the purchase in his database.
Unsign_message() –  used by the broker to decrypt the  signed message sent by the client.





