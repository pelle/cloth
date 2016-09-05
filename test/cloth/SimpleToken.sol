contract SimpleToken {
  uint32 public circulation;
  address public issuer;
  address[] public customers;
  mapping (address => uint32) public balances;
  mapping (address => uint) public authorized;
  string public message;
  bytes32 public ipfs;

  event Issued(address indexed recipient, uint32 amount);
  event Message(address indexed shouter, string message);
  event Transferred(address indexed sender, address indexed recipient, uint32 amount);

  function SimpleToken() {
    issuer = msg.sender;
  }

  modifier onlyIssuer() { if (msg.sender == issuer) _ }
  modifier sufficientFunds(uint32 amount) { if (amount <= balances[msg.sender]) _ }
  modifier authorizedCustomer(address customer) { if (authorized[customer] != 0 ) _ }
  modifier unAuthorizedCustomer(address customer) { if (authorized[customer] == 0 ) _ }

  function customer(address customer) constant
    returns(uint authorizedTime, uint32 balance) {
    return (authorized[customer], balances[customer]);
  }

  function authorize(address customer) public
    onlyIssuer
    unAuthorizedCustomer(customer)
    returns(bool success) {
        authorized[customer] = block.timestamp;
        customers.push(customer);
        return true;
  }

  function issue(address recipient, uint32 amount) public
    onlyIssuer
    returns(bool success) {
        circulation = amount;
        balances[recipient] = amount;
        Issued(recipient, amount);
        return true;
  }

  function setMessage(string _message)
    public returns(string message) {
        message = _message;
        Message(msg.sender,message);
        return message;
  }

  function getCustomers() constant returns(address[]) {
    return customers;
  }

  function transfer(address recipient, uint32 amount) public
    authorizedCustomer(msg.sender)
    sufficientFunds(amount)
    returns(bool success) {
    balances[msg.sender] -= amount;
    balances[recipient] += amount;
    Transferred(msg.sender, recipient, amount);
    return true;
  }

}
