package Test.RTU

import Base.toSimpleHex
import KModbus.*
import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec
import java.nio.ByteBuffer


class MdCtrls : ModbusCtrls {
    override fun IDCheck(id: Int): Boolean {
        return id>1
    }

    override fun DBRead(id: Int, dbType: MdRegion, addr: Int): Int {
        if(dbType == MdRegion.MD_REGION_1XXXX || dbType == MdRegion.MD_REGION_2XXXX){
            return addr and 0x01
        }
        return dbType.toCode() * 10000 + id * 1000 + addr
    }

    override fun DBWrite(id: Int, dbType: MdRegion, addr: Int, data: Short): Boolean {
        println("W:\t$id\t@\t$addr -> $data")
        return true
    }

    override fun HeaderFind(buffer: ByteBuffer): Boolean {
        return true
    }
}

val station = Station(
        IsSlave = false,
        Ctrl = MdCtrls())


//fun KM_RTU_Master_Setup(st: Station,id:Int,cmd: MdCmdEnum,trgAddr:Int,trgLen:Int):ByteArray{

class RTU1 : StringSpec() {
    init {
        var tmp1 = KM_RTU_Master_Setup(
                st = station,
                id = 0x12,
                cmd = MdCmdEnum.RD_WORD,
                trgAddr = 0x1234,
                trgLen = 0x12
        )
        "RD_WORD" {
            tmp1.toSimpleHex() shouldBe "12031234001283D2"
        }
    }
}

class RTU2 : StringSpec() {
    init {
        val tmp2 = KM_RTU_Master_Setup(
                st = station,
                id = 0x12,
                cmd = MdCmdEnum.RD_WORD_RO,
                trgAddr = 0x1234,
                trgLen = 0x12
        )
        "RD_WORD_RO" {
            tmp2.toSimpleHex() shouldBe "1204123400123612"
        }
    }
}
class RTU3 : StringSpec() {
    init {
        val tmp3 = KM_RTU_Master_Setup(
                st = station,
                id = 0x12,
                cmd = MdCmdEnum.RD_BITS,
                trgAddr = 0x1234,
                trgLen = 0x12
        )
        "RD_BITS" {
            tmp3.toSimpleHex() shouldBe "120112340012FA12"
        }
    }
}
class RTU4 : StringSpec() {
    init {
        val tmp4= KM_RTU_Master_Setup(
                st = station,
                id = 0x12,
                cmd = MdCmdEnum.RD_BITS_RO,
                trgAddr = 0x1234,
                trgLen = 0x12
        )
        "RD_BITS_RO" {
            tmp4.toSimpleHex() shouldBe "120212340012BE12"
        }
    }
}
class RTU5 : StringSpec() {
    init {
        val tmp5= KM_RTU_Master_Setup(
                st = station,
                id = 0x12,
                cmd = MdCmdEnum.WR_BITS_ONE,
                trgAddr = 0x1234,
                trgLen = 0x4321
        )
        "WR_BITS_ONE" {
            tmp5.toSimpleHex() shouldBe "1205123400008BDF"
        }
    }
}

class RTU51 : StringSpec() {
    init {
        val tmp51= KM_RTU_Master_Setup(
                st = station,
                id = 0x12,
                cmd = MdCmdEnum.WR_BITS_ONE,
                trgAddr = 0x1234,
                trgLen = 0x00
        )
        "WR_BITS_ONE" {
            tmp51.toSimpleHex() shouldBe "1205123400008BDF"
        }
    }
}

class RTU6 : StringSpec() {
    init {
        val tmp6= KM_RTU_Master_Setup(
                st = station,
                id = 0x12,
                cmd = MdCmdEnum.WR_WORD_ONE,
                trgAddr = 0x1234,
                trgLen = 0x4321
        )
        "WR_WORD_ONE" {
            tmp6.toSimpleHex() shouldBe "12061234F4C4888C"
        }

    }
}

class RTU7 : StringSpec() {
    init {
        val tmp7= KM_RTU_Master_Setup(
                st = station,
                id = 0x12,
                cmd = MdCmdEnum.WR_WORD_SOME,
                trgAddr = 0x1234,
                trgLen = 0x12
        )
        "WR_WORD_SOME" {
            tmp7.toSimpleHex() shouldBe "12101234001224F4C4F4C5F4C6F4C7F4C8F4C9F4CAF4CBF4CCF4CDF4CEF4CFF4D0F4D1F4D2F4D3F4D4F4D5548E"
        }
    }
}

class RTU8 : StringSpec() {
    init {
        val tmp8= KM_RTU_Master_Setup(
                st = station,
                id = 0x12,
                cmd = MdCmdEnum.WR_BITS_SOME,
                trgAddr = 0x1235,
                trgLen = 0x12
        )
        "WR_BITS_SOME" {
            tmp8.toSimpleHex() shouldBe "120F123500120355550122B9"
        }
    }
}







