
import Base.toUInt
import java.nio.ByteBuffer




fun main(args: Array<String>) {
//    Observable.fromArray(arrayOf(1,2,3)).doOnNext { println("start$it") }
//    println("start")
    val array = byteArrayOf(
            0xFF.toByte(),
            0xFF.toByte(),
            0xFF.toByte(),
            0xFF.toByte(),
            0xFF.toByte(),
            0x01,
            0x02,
            0x03,
            0x04
    )
    val bb = ByteBuffer.wrap(array)
    println("${bb.getShort(0).toUInt()},${bb.getShort(5)}")
    println("byte test ${(65535/256).toByte()} ${(65535%256).toByte()}")
    for (tmp in 0..0){
        println("I am OK")
        println("${255.toByte()}")
    }

//    println(URLEncoder.encode("/EUS/20180320/ZZ16211412!484张福志/1/Video/2019000000012288_report.jpg", "UTF-8"))
}