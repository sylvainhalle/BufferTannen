BufferTannen: a protocol
========================

(User Manual, version 2013-10-25)

BufferTannen is a Java software package that allows the serialization and
transmission of structured data over limited communication channels. It is
composed of two parts:

- A set of **classes** allowing the representation of structured data in a
  compact binary form. Contrarily to other systems, like Google's
  [protocol buffers](http://code.google.com/p/protobuf/), defining new
  message types can be done at runtime and does not require compiling new
  classes to be used. Moreover, messages in
  BufferTannen cannot be encoded and decoded without prior knowledge of
  their structure. However, since messages do not contain information about
  their structure, they use much less space.
- A **protocol** allowing the transmission of such messages. The protocol is
  specifically designed to operate on lossy, low-bandwidth and one-way
  communication channels.

Although BufferTannen is written in Java, its serialization mechanism and
protocol could interoperate with implementations written in other languages
(although none exists at the moment).

Table of contents                                                    {#toc}
-----------------

- [Messages and Schemas](#messages)
- [Reading and Writing Messages](#read-write)
- [Transmitting Messages](#transmitting)
- [Encoding Details](#encoding-details)
- [Protocol Details](#protocol-details)
- [Delta Segments](#delta-segments)
- [Why BufferTannen?](#why)
- [About the Author](#about)

Messages and Schemas
--------------------

The basic unit of information in BufferTannen is called a *message*. A
message is an instance of some data structure, filled with actual data.
The declaration of a data structure is called a *schema*.

Information can be represented in three different forms:

- Smallscii string: a variable-length string of characters. Since
  BufferTannen is aimed towards limiting as much as possible the number of
  bits required to represent information, these strings are restricted to a
  subset of 63 ASCII characters (lowercase letters, digits and punctuation).
  Each character in a Smallscii string takes 6 bits, and each string ends
  with the 6-bit string "000000".
- Integer: the only numerical type available in BufferTannen. When declared,
  integers are given a "width", i.e. the number of bits used to encode them.
  The width can be anything between 1 and 16 bits.
- Enumeration: a list of predefined Smallscii constants. Enumerations can
  be used to further reduce the amount of space taken by a message when the
  range of possible values for an element is known in advance. For example,
  if an element can only take values "north", "south", "east" or "west", it
  is better to encode this value as an enumeration (which takes 2 bits) than
  as a free-form Smallscii string (which would take at least 30 bits).
  As we shall see, while enumerations are encoded internally as integers,
  they are exposed to the user as if they were strings.
 
These basic building blocks can be used to write schemas by combining them
using compound data structures:

- List: a variable-length sequence of elements, all of which must be of the
  same type (or schema). List elements are accessed by their index, starting
  with index 0.
- FixedMap: a table that associates strings to values. The structure is
  fixed: the exact strings that can be used as keys must be declared;
  however, each key can be associated to a value of a different type.

These constructs can be mixed freely. The following represents the
declaration of a complex message schema:

    FixedMap {
      "title" : Smallscii,
      "price" : Integer(5),
      "chapters" : List [
        FixedMap {
          "name" : Smallscii,
          "length" : Integer(8),
          "type" : Enum {"normal", "appendix"}
        }
      ]
    }

The top-level structure for this message is a map (delimited by `{`...`}`).
This map has three keys: "title", whose associated value is a Smallscii
string, "price", whose associated value is a integer in the range 0-32
(i.e. 5 bits), and "chapters", whose value is not a primitive type, but is
itself a list (delimited by `[`...`]`). Each element of this list is itself
a map with three keys: a string "name", an integer "length", and "type"
whose possible values are "normal" or "appendix".

A message is an instance of a schema. For example, the following is a
possible message abiding by the previous schema:

    {
      "title" : "hello world",
      "price" : 21,
      "chapters" : [
        {
          "name" : "chapter 1",
          "length" : 3,
          "type" : "normal"
        },
        {
          "name" : "chapter 2",
          "length" : 7,
          "type" : "normal"
        },
        {
          "name" : "conclusion",
          "length" : 2,
          "type" : "appendix"
        }
      ]
    }

The reader familiar with JSON or similar notations will notice strong
similarities between BufferTannen and these languages. As a matter of fact,
elements of a message can be queried using a syntax similar to JavaScript.
For example, assuming that `m` is an object representing the above message,
fetching the length of the second chapter would be written as the
expression:

    m[chapters][1][length]

This fetches the `chapters` value in the top-level structure (a list), then
the second element of that list (index 1), and then the `length` value of
the corresponding map element.

[Back to top](#toc)

Reading and Writing Messages                                  {#read-write}
----------------------------

In BufferTannen, both schemas and instances of schemas are represented by
the same object, called `SchemaElement`. An empty SchemaElement must first
be instantiated using some schema; this can be done by either:

- Reading a character string formatted as above, using the static method
  `SchemaElement.parseSchemaFromString(s)`, which returns the desired
  element; or
- Reading a binary string containing an encoding of the schema, this time
  using the static method `SchemaElement.parseSchemaFromBitSequence(s)`.
  As a matter of fact, in BufferTannen both messages *and* schemas can be
  transmitted in binary form over a communication channel, and a method is
  provided to export the schema of some message into a sequence of bits.

Once an empty SchemaElement `e` is obtained, it can be filled with data,
again in two ways:

- By reading a character string formatted as above, using the method
  `sch.readContentsFromString(s)`; or
- By reading a binary string containing an encoding of the data, using the
  method `sch.fromBitSequence(s)`

Similar methods exist to operate in the opposite way, and to *write* a
message's schema or data contents either as a character string or as a
binary string. This way, messages and schemas can be freely encoded/decoded
using human-readable text strings or compact binary strings.

As one can see, for a message to be read or written, it is necessary first
to instantiate an object with a schema. As a matter of fact, calling
`sch.readContentsFromString(s)` or `sch.fromBitSequence(s)` without first
telling the object `sch` what is the underlying schema to use will cause an
error, even if `s` contains properly formatted data. Similarly, trying to
read data that uses some schema with an object instantiated with another
schema will also cause an error. In other words, no data can be read or
written without knowledge of the proper schema to use.

This might seem restrictive, but it allows BufferTannen to heavily optimize
the binary representation of messages. In the absence of a known schema,
each message would require to carry, in addition to its actual data,
information about its own structure. Since a reader receiving a sequence
of bits would not know in advance how to read it, special sequences would
need to be added to notify the reader that what follows is a map with some
number of keys, and then in turn each value for each key would also need to
declare the structure of its own type, and so on.

Partically speaking, this amounts to repeating within each message the
description of its schema, interspersed through the message data. On the
contrary, if the schema is known, all this signalling information can be
discarded: when receiving a sequence of bits, a reader that possesses the
schema knows exactly how many bits to read, what data this represents and
where to place it in the message structure being populated. This entails,
however, that a receiver that does not know the schema to apply has no clue
whatsoever on how to process a binary string.

[Back to top](#toc)

Transmitting Messages                                       {#transmitting}
---------------------

BufferTannen also provides a mechanism for transmitting messages over a
binary communication channel. Although any channel (TCP connection, etc.)
can be used, BufferTannen was designed to operate on a channel with the
following specifications:

- The channel is **point-to-point**. The goal is to send information
  directly from A to B; no addressing, routing, etc. is provided.
- The channel is **low-bandwidth** (that is, able to transmit a few hundred
  bytes at a time, possibly less than 10 times per second).
- The channel is **one-way**: typically, one side of the communication sends
  data that is to be picked up by some receiver. This entails that the
  receiver cannot acknowledge reception of data or ask the sender to
  transmit again, as in protocols like TCP.
- The channel is **lossy**. However, we assume that the channel provides a
  mechanism (such as some form of checksum) to detect when a piece of data
  is corrupted and discard it.
- A receiver can start listening on the channel at any time, and be able to
  correctly receive messages from that point on. As such, the communication
  does not have a formal "start" that could be used, for example, to
  advertise parameters used for the exchange.

Therefore, the communication channel envisioned as the transmission medium
for BufferTannen's messages can be likened in many ways to a slow broadcast
signal, such as [slow-scan television](http://en.wikipedia.org/wiki/Slow-scan_television),
[Hellschreiber](http://en.wikipedia.org/wiki/Hellschreiber), or
[RDS](http://en.wikipedia.org/wiki/Radio_Data_System).

BufferTannen's protocol aims at transmitting messages as reliably as
possible under these conditions, while preserving the integrity of data and
the ordering of messages. The low-bandwidth nature of the channel
explains the emphasis on serializing messages in a compact binary form.
Since the receiver cannot ask for any form of retransmission, the protocol
must provide for automatic retransmissions of each message to maximize their
chances of being picked up, while at the same time not confusing a
retransmission with a new message with identical content. Moreover, as the
receiver can start listening at any moment, and that the schema of messages
must be known in order to decode them, the schemas used in the communication
must also be transmitted at periodical intervals.

BufferTannen provides an implementation of a sender and a receiver that
transparently handle these different aspects.

### Segments and Frames

Messages and schemas are encapsulated into a structured called a *segment*.
A segment can be of two types:

- A *schema segment* contains the binary representation of a schema, which
  is associated to a number. Multiple schemas can be used in the same
  communication, hence creating a bank of schemas identified by their
  number.
- A *message segment* contains the binary representation of a message, along
  with a sequential number (used to preserve the ordering of messages
  received), as well as the number referring to the schema that must be used
  to decode the message.
- A *delta segment* contains the binary representation of a message,
  expressed as the difference ("delta") between that message and a previous
  one used as a reference. Delta segments are used to further compress the
  representation of a message, in the case where messages don't change much
  over an interval of time.
- A fourth type of segment, *blob*, is currently left unimplemented. It is
  intended to carry raw binary data over the BufferTannen protocol.

The communication channel sends binary data in units called *frames*. A
frame is simply a set of concatenated segments in binary form, preceded by
a header containing the version number of the protocol (currently "1") and
the length (in bits) of the frame's content. When many segments are awaiting
to be transmitted, the protocol tries to fit as many segments as possible
(in sequential order) within the maximum size of a frame before sending it.
This maximum size, currently set at 4096 bits (512 bytes), can be modified
to fit the specifics of the communication channel that is being used.

In the current version of the protocol, segments cannot be fragmented
across multiple frames. Hence a segment cannot exceed the maximum size of a
frame.

### Using the Sender

The `Sender` class provides functionalities to handle send operations:

- Method `addMessage` takes as input a SchemaElement representing a message
  (with data) that needs to be sent over the communication channel. One
  should call this method whenever a new message must be sent.
- Method `setSchema` allows the user to associate a schema to a number in
  the schema bank.
- Method `pollBitSequence` should be called at periodic intervals. It
  returns a sequence of bits whenever one is ready to be sent over the
  communication channel (it returns `null` when nothing is to be sent at
  the moment).

The sender transparently handles the sending of schema segments at intervals
and the retransmission of message segments. For more details on the
protocol, see the [protocol details](#protocol-details). It is up to the
user to link the return value of method `pollBitSequence` with the sending
process of the communication channel, whatever that may be.

### Using the Receiver

The `Receiver` class provides functionalities to handle receive operations:

- Method `putBitSequence` takes as input a sequence of bits representing
  a frame, as received from the communication channel. It is up to the
  user to link the receiving process of the communication channel (whatever
  that may be) with the input of method `putBitSequence`.
- Method `pollMessage` should be called at periodic intervals. It returns a
  properly decoded SchemaElement object whenever one is ready.
  
The receiver transparently handles the buffering of segments when received
out of sequence and the management of the schema bank used to decode
messages (as a matter of fact, the end-user of the Receiver class never
needs to deal with schema segments).

Since the communication channel is inherently lossy, it is possible that a
message, despite being retransmitted (a finite number of times), ultimately
does not get picked up by the receiver. In such a case, the receiver marks
that message as lost and increments a counter of messages lost. This counter
can be queried by using method `getMessageLostCount`. By calling this method
at the same time as `pollMessage` is called, the receiver can deduce whether
messages have been lost between the one being processed and the last one.

[Back to top](#toc)

Encoding Details                                        {#encoding-details}
----------------

TODO

[Back to top](#toc)

Protocol Details                                        {#protocol-details}
----------------

TODO

[Back to top](#toc)

Delta Segments                                            {#delta-segments}
---------------

TODO

[Back to top](#toc)

Why BufferTannen?                                                    {#why}
----------------

As a pun on [Buford Tannen](http://backtothefuture.wikia.com/wiki/Buford_Tannen),
Marty McFly's rival in *Back to the Future Part III*.

About the Author                                                   {#about}
----------------

BufferTannen was initially developed by Sylvain Hallé, currently an
Assistant Professor at [Université du Québec à Chicoutimi,
Canada](http://www.uqac.ca/) and head of [LIF](http://lif.uqac.ca/), the
Laboratory of Formal Computer Science ("Laboratoire d'informatique
formelle").

[Back to top](#toc)
