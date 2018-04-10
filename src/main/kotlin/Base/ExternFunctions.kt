package Base

import java.nio.ByteBuffer
import kotlin.experimental.and


fun Int.toPaddedHex(pad:Int = 4):String{
    val tmp = "000000000000000"+this.toString(16).toUpperCase()
    return tmp.takeLast(pad)
}
fun Long.toPaddedHex(pad:Int = 4):String{
    val tmp = "000000000000000"+this.toString(16).toUpperCase()
    return tmp.takeLast(pad)
}
fun Int.toHex(pad:Int = 4):String{
    val tmp = "000000000000000"+this.toString(16).toUpperCase()
    return "0x"+tmp.takeLast(pad)
}

fun Byte.toUnsigned():Short{
    if(this <0){
        return (256+this).toShort()
    }
    return this.toShort()
}


fun ByteArray.toHex(): String {
    var Str = ""
    var cnt = 0
    this.forEach {
        Str += (String.format("%02X",it)+" ")
        if((cnt++ % 8) == 7) {
            Str += "\r\n"
        }
    }
    return Str
}


fun ByteArray.toSimpleHex(): String {
    var Str = ""
    this.forEach {
        Str += (String.format("%02X",it)+"")
    }
    return Str
}

fun ByteArray.getInt(index:Int = 0): Int {
    if (this.size <= index+1){
        return 0
    }
    return (this[index + 0].toUnsigned() * 256 + this[index + 1].toUnsigned())
}

fun ByteArray.getBit(index:Int = 0): Int {
    if (this.size*8 <= index){
        return 0
    }
    return if(this[index/8].toUnsigned().toInt().and(1 shl index%8) >0){
        1
    }else{
        0
    }
}

/**
 *
 */
fun ByteBuffer.getByteArray(offset:Int, size:Int): ByteArray {

    val ret = ByteArray(size)
    for(tmp in 0..size-1){
        ret[tmp] = this[tmp+offset]
    }
    return ret
}



/**
 * Clean string: trim and remove ending.
 * @param {String} string The string to clean.
 * @return {String} The cleaned string.
 */
fun String.clean(): String {
    return this.trim().replace("\u200B","")
}


fun ByteBuffer.getBit(index:Int):Short{
    return this.get(index/8).toInt().ushr(index%8).and(0x01).toShort()
}

fun Short.toUInt():Int{
    if(this<0){
        return 65536+this
    }
    return this.toInt()
}

fun String.Parse2HexBytes(){
    val ret = ByteArray( (this.length+1)/2)
    for (tmp in 0..(ret.size - 1)){
        val hex = this.toByte(16)
    }
}







