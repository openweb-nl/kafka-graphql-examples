# Expose your Kafka using GraphQl

------ intro -------

GraphQL just a data language. Database can be anything as with rest.
Connecting to other servics can be same thing as with rest.
There is not something like subscriptions for rest.
Subscription allow to stream data from the server to a client.
It's a trade-off having a stateless rest server makes caching easy.
With rest you could periodically call Rest.

------ demo -------

Bank simulation

------ towards production -------

Key based on user, have all traffic  connect to correct initial one.
Use Kafka connect to fill the database.
Possible to usee interceptors for security.

------ conclusion -------