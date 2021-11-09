# Intro
This project can let user make NFT on minecraft server.

# Video Demo 
Google Drive: https://drive.google.com/file/d/1yGOCCTYb7l0n6MBRMu-KML0ViaR_w6Wy/view?usp=sharing
forgive me the record quility.

# Folder
- `java` java extension based on paper mc
- `minecraft-nft` smart contract

# how to
## pre

make shure you have `terrad`(on the $PATH) `localterra`

use
```
mvn compile
mvn install
```
to get the plugin on target

start a minecraft server with plugin by using paper mc.

compile the contract and uplaod it

## in game
use `\check` on chat to check item on the hand can be mint or not
use `\mint` to mint item on hand to NFT, if an item mint to NFT, this item will be unbreakable.

## prevent hack
when initiate the contract will record who create this.
it must be server initiate contract and user get it.

So the on the state the creator is server and the owner is uesr.

# TODO on the future
- [  ] user transfer
