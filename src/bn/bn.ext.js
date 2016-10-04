  function BN (number, base, endian) {
  }
  BN.BN = BN;
  BN.wordSize = 26;

  BN.isBN = function isBN (num) {
  };

  BN.max = function max (left, right) {
  };

  BN.min = function min (left, right) {
  };

  BN.prototype.copy = function copy (dest) {
  };

  BN.prototype.clone = function clone () {
  };

  BN.prototype.strip = function strip () {
  };

  BN.prototype._normSign = function _normSign () {
  };

  BN.prototype.inspect = function inspect () {
  };

  BN.prototype.toString = function toString (base, padding) {
  };

  BN.prototype.toNumber = function toNumber () {
  };

  BN.prototype.toJSON = function toJSON () {
  };

  BN.prototype.toBuffer = function toBuffer (endian, length) {
  };

  BN.prototype.toArray = function toArray (endian, length) {
  };

  BN.prototype.toArrayLike = function toArrayLike (ArrayType, endian, length) {
  };

  BN.prototype.bitLength = function bitLength () {
  };

  BN.prototype.zeroBits = function zeroBits () {
  };

  BN.prototype.byteLength = function byteLength () {
  };

  BN.prototype.toTwos = function toTwos (width) {
  };

  BN.prototype.fromTwos = function fromTwos (width) {
  };

  BN.prototype.isNeg = function isNeg () {
  };

  // Return negative clone of `this`
  BN.prototype.neg = function neg () {
  };

  BN.prototype.ineg = function ineg () {
  };

  // Or `num` with `this` in-place
  BN.prototype.iuor = function iuor (num) {
  };

  BN.prototype.ior = function ior (num) {
  };

  // Or `num` with `this`
  BN.prototype.or = function or (num) {
  };

  BN.prototype.uor = function uor (num) {
  };

  // And `num` with `this` in-place
  BN.prototype.iuand = function iuand (num) {
  };

  BN.prototype.iand = function iand (num) {
  };

  // And `num` with `this`
  BN.prototype.and = function and (num) {
  };

  BN.prototype.uand = function uand (num) {
  };

  // Xor `num` with `this` in-place
  BN.prototype.iuxor = function iuxor (num) {
  };

  BN.prototype.ixor = function ixor (num) {
  };

  // Xor `num` with `this`
  BN.prototype.xor = function xor (num) {
  };

  BN.prototype.uxor = function uxor (num) {
  };

  // Not ``this`` with ``width`` bitwidth
  BN.prototype.inotn = function inotn (width) {
  };

  BN.prototype.notn = function notn (width) {
  };

  // Set `bit` of `this`
  BN.prototype.setn = function setn (bit, val) {
  };

  // Add `num` to `this` in-place
  BN.prototype.iadd = function iadd (num) {
  };

  // Add `num` to `this`
  BN.prototype.add = function add (num) {
  };

  // Subtract `num` from `this` in-place
  BN.prototype.isub = function isub (num) {
  };

  // Subtract `num` from `this`
  BN.prototype.sub = function sub (num) {
  };

  BN.prototype.mulTo = function mulTo (num, out) {
  };

  // Multiply `this` by `num`
  BN.prototype.mul = function mul (num) {
  };

  // Multiply employing FFT
  BN.prototype.mulf = function mulf (num) {
  };

  // In-place Multiplication
  BN.prototype.imul = function imul (num) {
  };

  BN.prototype.imuln = function imuln (num) {
  };

  BN.prototype.muln = function muln (num) {
  };

  // `this` * `this`
  BN.prototype.sqr = function sqr () {
  };

  // `this` * `this` in-place
  BN.prototype.isqr = function isqr () {
  };

  // Math.pow(`this`, `num`)
  BN.prototype.pow = function pow (num) {
  };

  // Shift-left in-place
  BN.prototype.iushln = function iushln (bits) {
  };

  BN.prototype.ishln = function ishln (bits) {
  };

  // Shift-right in-place
  // NOTE: `hint` is a lowest bit before trailing zeroes
  // NOTE: if `extended` is present - it will be filled with destroyed bits
  BN.prototype.iushrn = function iushrn (bits, hint, extended) {
  };

  BN.prototype.ishrn = function ishrn (bits, hint, extended) {
  };

  // Shift-left
  BN.prototype.shln = function shln (bits) {
  };

  BN.prototype.ushln = function ushln (bits) {
  };

  // Shift-right
  BN.prototype.shrn = function shrn (bits) {
  };

  BN.prototype.ushrn = function ushrn (bits) {
  };

  // Test if n bit is set
  BN.prototype.testn = function testn (bit) {
  };

  // Return only lowers bits of number (in-place)
  BN.prototype.imaskn = function imaskn (bits) {
  };

  // Return only lowers bits of number
  BN.prototype.maskn = function maskn (bits) {
  };

  // Add plain number `num` to `this`
  BN.prototype.iaddn = function iaddn (num) {
  };

  BN.prototype._iaddn = function _iaddn (num) {
  };

  // Subtract plain number `num` from `this`
  BN.prototype.isubn = function isubn (num) {
  };

  BN.prototype.addn = function addn (num) {
  };

  BN.prototype.subn = function subn (num) {
  };

  BN.prototype.iabs = function iabs () {
  };

  BN.prototype.abs = function abs () {
  };

  BN.prototype.divmod = function divmod (num, mode, positive) {
  };

  // Find `this` / `num`
  BN.prototype.div = function div (num) {
  };

  // Find `this` % `num`
  BN.prototype.mod = function mod (num) {
  };

  BN.prototype.umod = function umod (num) {
  };

  // Find Round(`this` / `num`)
  BN.prototype.divRound = function divRound (num) {
  };

  BN.prototype.modn = function modn (num) {
  };

  // In-place division by number
  BN.prototype.idivn = function idivn (num) {
  };

  BN.prototype.divn = function divn (num) {
  };

  BN.prototype.egcd = function egcd (p) {
  };

  BN.prototype.gcd = function gcd (num) {
  };

  // Invert number in the field F(num)
  BN.prototype.invm = function invm (num) {
  };

  BN.prototype.isEven = function isEven () {
  };

  BN.prototype.isOdd = function isOdd () {
  };

  // And first word and num
  BN.prototype.andln = function andln (num) {
  };

  // Increment at the bit position in-line
  BN.prototype.bincn = function bincn (bit) {
  };

  BN.prototype.isZero = function isZero () {
  };

  BN.prototype.cmpn = function cmpn (num) {
  };

  // Compare two numbers and return:
  // 1 - if `this` > `num`
  // 0 - if `this` == `num`
  // -1 - if `this` < `num`
  BN.prototype.cmp = function cmp (num) {
  };

  // Unsigned comparison
  BN.prototype.ucmp = function ucmp (num) {
  };

  BN.prototype.gtn = function gtn (num) {
  };

  BN.prototype.gt = function gt (num) {
  };

  BN.prototype.gten = function gten (num) {
  };

  BN.prototype.gte = function gte (num) {
  };

  BN.prototype.ltn = function ltn (num) {
  };

  BN.prototype.lt = function lt (num) {
  };

  BN.prototype.lten = function lten (num) {
  };

  BN.prototype.lte = function lte (num) {
  };

  BN.prototype.eqn = function eqn (num) {
  };

  BN.prototype.eq = function eq (num) {
  };

  BN.red = function red (num) {
  };

  BN.prototype.toRed = function toRed (ctx) {
  };

  BN.prototype.fromRed = function fromRed () {
  };

  BN.prototype.forceRed = function forceRed (ctx) {
  };

  BN.prototype.redAdd = function redAdd (num) {
  };

  BN.prototype.redIAdd = function redIAdd (num) {
  };

  BN.prototype.redSub = function redSub (num) {
  };

  BN.prototype.redISub = function redISub (num) {
  };

  BN.prototype.redShl = function redShl (num) {
  };

  BN.prototype.redMul = function redMul (num) {
  };

  BN.prototype.redIMul = function redIMul (num) {
  };

  BN.prototype.redSqr = function redSqr () {
  };

  BN.prototype.redISqr = function redISqr () {
  };

  // Square root over p
  BN.prototype.redSqrt = function redSqrt () {
  };

  BN.prototype.redInvm = function redInvm () {
  };

  // Return negative clone of `this` % `red modulo`
  BN.prototype.redNeg = function redNeg () {
  };

  BN.prototype.redPow = function redPow (num) {
  };
  BN.mont = function mont (num) {
  };
