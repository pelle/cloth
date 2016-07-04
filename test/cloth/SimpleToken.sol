contract SimpleToken {
  uint public circulation;
  address public issuer;
  address[] public members;
  mapping (address=> uint) accounts;

  event Transfered(address indexed recipient, uint amount);

  function SimpleToken() {
    issuer = msg.sender;
  }

  modifier onlyIssuer() { if (msg.sender == issuer) _ }
  modifier sufficientFunds(uint amount) { if (amount <= accounts[msg.sender]) _ }

  function issue(address recipient, uint amount) public onlyIssuer {
    circulation += amount;
    accounts[recipient] += amount;
    members.push(recipient);
    Transfered(recipient, amount);
  }

  function transfer(address recipient, uint amount) public sufficientFunds(amount) {
    accounts[msg.sender] -= amount;
    accounts[recipient] += amount;
    Transfered(recipient, amount);
  }

}
