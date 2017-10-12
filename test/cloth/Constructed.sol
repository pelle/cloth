pragma solidity ^0.4.16;

contract Constructed {
    string public status;

    function Constructed(string _status) {
        status = _status;
    }
}
