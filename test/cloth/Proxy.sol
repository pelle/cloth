pragma solidity ^0.4.1;
// This is a simple proxy for testing purposes. Real life proxy contracts will have a lot more information
contract Proxy {
    address public owner;
    function Proxy() {
        owner = msg.sender;
    }

    function() payable { }

    function forward(address recipient, uint value, bytes data) {
        if (msg.sender == owner) {
           if (!recipient.call.value(value)(data)) {
                throw;
            }
        }
     }
}
