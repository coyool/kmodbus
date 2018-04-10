package Test.RTU

import Base.getBit
import Base.getInt
import Base.toSimpleHex
import Base.toUInt
import KModbus.*
import Test.TCP.PDCtrls1
import Test.TCP.PDCtrls2
import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec
import java.nio.ByteBuffer

//I used map to test for convenience, you are recommended to use ByteBuffer
val globalMapOfValue : MutableMap<Int,Int> = mutableMapOf()


//PD == Package Deal
class PDCtrls1 : ModbusCtrls {
    override fun IDCheck(id: Int): Boolean {
        return id == 1
    }

    override fun DBRead(id: Int, dbType: MdRegion, addr: Int): Int {
        if(dbType == MdRegion.MD_REGION_1XXXX || dbType == MdRegion.MD_REGION_2XXXX){
            return addr and 0x01
        }
        return dbType.toCode() * 10000 + id * 1000 + addr
    }

    override fun DBWrite(id: Int, dbType: MdRegion, addr: Int, data: Short): Boolean {
//        println("W:\t$id\t@\t$addr -> $data")
        Test.TCP.globalMapOfValue.put(addr,data.toUInt())
        return true
    }

    override fun HeaderFind(buffer: ByteBuffer): Boolean {
        return true
    }
}

class PDCtrls2 : ModbusCtrls {
    private val stations = listOf(2,4,6,8,10)
    override fun IDCheck(id: Int): Boolean {
        return id in stations
    }

    override fun DBRead(id: Int, dbType: MdRegion, addr: Int): Int {
        if(dbType == MdRegion.MD_REGION_1XXXX || dbType == MdRegion.MD_REGION_2XXXX){
            return addr and 0x01
        }
        return dbType.toCode() * 10000 + id * 1000 + addr
    }

    override fun DBWrite(id: Int, dbType: MdRegion, addr: Int, data: Short): Boolean {
        Test.TCP.globalMapOfValue.put(addr,data.toUInt())
        return true
    }

    override fun HeaderFind(buffer: ByteBuffer): Boolean {
        return true
    }
}

val PDStation1 = Station(
        IsSlave = false,
        Ctrl = PDCtrls1())

val PDStation2 = Station(
        IsSlave = true,
        Ctrl = PDCtrls2())




//fun KM_RTU_Master_Setup(st: Station,id:Int,cmd: MdCmdEnum,trgAddr:Int,trgLen:Int):ByteArray{

class TEST_RD : StringSpec() {
    init {
        val PDBus = StationBus(
                RawPkgLen = 0,
                RawBytes  = ByteArray(256),
                RspPkgLen = 0,
                Stations  = listOf(Test.TCP.PDStation1, Test.TCP.PDStation2)
        )
        PDBus.RawBytes = KM_RTU_Master_Setup(
                st = Test.TCP.PDStation1,
                id = 0x01,
                cmd = MdCmdEnum.RD_WORD,
                trgAddr = 0x1234,
                trgLen = 0x12
        )
        PDBus.RawPkgLen = PDBus.RawBytes.size
        "RawPkgLen Check"{
            PDBus.RawPkgLen shouldBe 8
        }

        "TEST_RD" {
            val ret = MD_RTU_Data_Deal(PDBus)
            ret.rsp.toSimpleHex() shouldBe "010324B25CB25DB25EB25FB260B261B262B263B264B265B266B267B268B269B26AB26BB26CB26D94AD"
            ret.code shouldBe MdRspCode.MD_ERR_OK
            for (tmp in 0..(0x12 - 1)){
                ret.rsp.getInt(tmp * 2 + RTU_RSP_RD_DATA_INDEX) shouldBe Test.TCP.PDStation1.Ctrl.DBRead(0x01,MdRegion.MD_REGION_4XXXX, 0x1234 + tmp)
            }
        }
    }
}

class TEST_RDO : StringSpec() {
    init {
        val PDBus = StationBus(
                RawPkgLen = 0,
                RawBytes  = ByteArray(256),
                RspPkgLen = 0,
                Stations  = listOf(Test.TCP.PDStation1, Test.TCP.PDStation2)
        )
        PDBus.RawBytes = KM_RTU_Master_Setup(
                st = Test.TCP.PDStation1,
                id = 0x01,
                cmd = MdCmdEnum.RD_WORD_RO,
                trgAddr = 20,
                trgLen  = 35
        )
        PDBus.RawPkgLen = PDBus.RawBytes.size
        "RawPkgLen Check"{
            PDBus.RawPkgLen shouldBe 8
        }

        "TEST_RDO" {
            val ret = MD_RTU_Data_Deal(PDBus)
            ret.rsp.toSimpleHex() shouldBe "010446792C792D792E792F7930793179327933793479357936793779387939793A793B793C793D793E793F7940794179427943794479457946794779487949794A794B794C794D794E7635"
            ret.code shouldBe MdRspCode.MD_ERR_OK
            for (tmp in 0..(35 - 1)){
                ret.rsp.getInt(tmp * 2 + RTU_RSP_RD_DATA_INDEX) shouldBe
                        Test.TCP.PDStation1.Ctrl.DBRead(0x01,MdRegion.MD_REGION_3XXXX, 20 + tmp)
            }
        }
    }
}

class TEST_RDC : StringSpec() {
    init {
        val PDBus = StationBus(
                RawPkgLen = 0,
                RawBytes  = ByteArray(256),
                RspPkgLen = 0,
                Stations  = listOf(Test.TCP.PDStation1, Test.TCP.PDStation2)
        )
        PDBus.RawBytes = KM_RTU_Master_Setup(
                st      = Test.TCP.PDStation1,
                id      = 2,
                cmd     = MdCmdEnum.RD_BITS,
                trgAddr = 20,
                trgLen  = 35
        )
        PDBus.RawPkgLen = PDBus.RawBytes.size
        "RawPkgLen Check"{
            PDBus.RawPkgLen shouldBe 8
        }

        "TEST_RDC" {
            val ret = MD_RTU_Data_Deal(PDBus)
            ret.rsp.toSimpleHex() shouldBe "020105AAAAAAAA0237C6"
            ret.code shouldBe MdRspCode.MD_ERR_OK
            for (tmp in 0..(35 - 1)){
                ret.rsp.getBit(tmp + RTU_RSP_RD_DATA_INDEX * 8) shouldBe
                        Test.TCP.PDStation1.Ctrl.DBRead(2,MdRegion.MD_REGION_1XXXX, 20 + tmp)
            }
        }
    }
}

class TEST_RDB : StringSpec() {
    init {
        val PDBus = StationBus(
                RawPkgLen = 0,
                RawBytes  = ByteArray(256),
                RspPkgLen = 0,
                Stations  = listOf(Test.TCP.PDStation1, Test.TCP.PDStation2)
        )
        PDBus.RawBytes = KM_RTU_Master_Setup(
                st      = Test.TCP.PDStation1,
                id      = 6,
                cmd     = MdCmdEnum.RD_BITS_RO,
                trgAddr = 200,
                trgLen  = 31
        )
        PDBus.RawPkgLen = PDBus.RawBytes.size
        "RawPkgLen Check"{
            PDBus.RawPkgLen shouldBe 8
        }

        "TEST_RB" {
            val ret = MD_RTU_Data_Deal(PDBus)
            ret.rsp.toSimpleHex() shouldBe "060204AAAAAA2A73A5"
            ret.code shouldBe MdRspCode.MD_ERR_OK
            for (tmp in 0..(31 - 1)){
                ret.rsp.getBit(tmp + RTU_RSP_RD_DATA_INDEX * 8) shouldBe
                        Test.TCP.PDStation1.Ctrl.DBRead(6,MdRegion.MD_REGION_2XXXX, 200 + tmp)
            }
        }
    }
}


//WR_BITS_SOME   , //write coils  region 1
//WR_WORD_ONE     , //write word   region 3
//WR_WORD_SOME    , //write some   region 3



class TEST_WR : StringSpec() {
    init {
        val PDBus = StationBus(
                RawPkgLen = 0,
                RawBytes  = ByteArray(256),
                RspPkgLen = 0,
                Stations  = listOf(Test.TCP.PDStation1, Test.TCP.PDStation2)
        )
        PDBus.RawBytes = KM_RTU_Master_Setup(
                st      = Test.TCP.PDStation1,
                id      = 6,
                cmd     = MdCmdEnum.WR_WORD_ONE,
                trgAddr = 200,
                trgLen  = 31
        )
        PDBus.RawPkgLen = PDBus.RawBytes.size
        "RawPkgLen Check"{
            PDBus.RawPkgLen shouldBe 8
        }

        "TEST_WR" {
            val ret = MD_RTU_Data_Deal(PDBus)
            ret.rsp.toSimpleHex() shouldBe "060600C8B4787EA1"
            ret.code shouldBe MdRspCode.MD_ERR_OK
            ret.rsp.getInt(RTU_REQ_SINGLE_DATA_INDEX) shouldBe
                    Test.TCP.PDStation1.Ctrl.DBRead(6,MdRegion.MD_REGION_4XXXX,200)
        }
    }
}


class TEST_WRC0 : StringSpec() {
    init {
        val PDBus = StationBus(
                RawPkgLen = 0,
                RawBytes  = ByteArray(256),
                RspPkgLen = 0,
                Stations  = listOf(Test.TCP.PDStation1, Test.TCP.PDStation2)
        )
        PDBus.RawBytes = KM_RTU_Master_Setup(
                st      = Test.TCP.PDStation1,
                id      = 6,
                cmd     = MdCmdEnum.WR_BITS_ONE,
                trgAddr = 200,
                trgLen  = 31
        )
        PDBus.RawPkgLen = PDBus.RawBytes.size
        "RawPkgLen Check"{
            PDBus.RawPkgLen shouldBe 8
        }

        "TEST_WR" {
            val ret = MD_RTU_Data_Deal(PDBus)
            ret.rsp.toSimpleHex() shouldBe "060500C800004D83"
            ret.code shouldBe MdRspCode.MD_ERR_OK
            ret.rsp.getBit(RTU_REQ_SINGLE_DATA_INDEX * 8) shouldBe
                    Test.TCP.PDStation1.Ctrl.DBRead(6,MdRegion.MD_REGION_1XXXX,200)
        }
    }
}

class TEST_WRC1 : StringSpec() {
    init {
        val PDBus = StationBus(
                RawPkgLen = 0,
                RawBytes  = ByteArray(256),
                RspPkgLen = 0,
                Stations  = listOf(Test.TCP.PDStation1, Test.TCP.PDStation2)
        )
        PDBus.RawBytes = KM_RTU_Master_Setup(
                st      = Test.TCP.PDStation1,
                id      = 6,
                cmd     = MdCmdEnum.WR_BITS_ONE,
                trgAddr = 201,
                trgLen  = 31
        )
        PDBus.RawPkgLen = PDBus.RawBytes.size
        "RawPkgLen Check"{
            PDBus.RawPkgLen shouldBe 8
        }

        "TEST_WR" {
            val ret = MD_RTU_Data_Deal(PDBus)
            ret.rsp.toSimpleHex() shouldBe "060500C9FF005DB3"
            ret.code shouldBe MdRspCode.MD_ERR_OK
            ret.rsp.getBit(RTU_REQ_SINGLE_DATA_INDEX * 8) shouldBe
                    Test.TCP.PDStation1.Ctrl.DBRead(6,MdRegion.MD_REGION_1XXXX,201)
        }
    }
}

class TEST_WRCP : StringSpec() {
    init {
        val PDBus = StationBus(
                RawPkgLen = 0,
                RawBytes  = ByteArray(256),
                RspPkgLen = 0,
                Stations  = listOf(Test.TCP.PDStation1, Test.TCP.PDStation2)
        )
        PDBus.RawBytes = KM_RTU_Master_Setup(
                st      = Test.TCP.PDStation1,
                id      = 6,
                cmd     = MdCmdEnum.WR_BITS_SOME,
                trgAddr = 201,
                trgLen  = 243
        )
        PDBus.RawPkgLen = PDBus.RawBytes.size
        Test.TCP.globalMapOfValue.clear()

        "RawPkgLen Check"{
            PDBus.RawPkgLen shouldBe (RTU_REQ_WRP_EXTRA_BYTE + (243 + 7) / 8)
        }

        "TEST_WRCP" {
            val ret = MD_RTU_Data_Deal(PDBus)
            ret.rsp.toSimpleHex() shouldBe "060F00C900F3C407"
            ret.rsp.size shouldBe 8
            ret.code shouldBe MdRspCode.MD_ERR_OK
            for (tmp in 0..(243 - 1)){
                Test.TCP.globalMapOfValue[tmp + 201] shouldBe Test.TCP.PDStation1.Ctrl.DBRead(6,MdRegion.MD_REGION_1XXXX, 201 + tmp)
            }
        }
    }
}


class TEST_WRP : StringSpec() {
    init {
        val PDBus = StationBus(
                RawPkgLen = 0,
                RawBytes  = ByteArray(256),
                RspPkgLen = 0,
                Stations  = listOf(Test.TCP.PDStation1, Test.TCP.PDStation2)
        )
        val test_id   = 6
        val test_addr = 335
        val test_numb = 115
        PDBus.RawBytes = KM_RTU_Master_Setup(
                st      = Test.TCP.PDStation2,
                id      = test_id,
                cmd     = MdCmdEnum.WR_WORD_SOME,
                trgAddr = test_addr,
                trgLen  = test_numb
        )
        PDBus.RawPkgLen = PDBus.RawBytes.size
        Test.TCP.globalMapOfValue.clear()

        "RawPkgLen Check"{
            PDBus.RawPkgLen shouldBe (RTU_REQ_WRP_EXTRA_BYTE + test_numb * 2)
        }

        "TEST_WRP" {
            val ret = MD_RTU_Data_Deal(PDBus)
            ret.rsp.toSimpleHex() shouldBe "0610014F0073B070"
            ret.rsp.size shouldBe RTU_SHORT_CMD_LEN
            ret.code shouldBe MdRspCode.MD_ERR_OK
            for (tmp in 0..(test_numb - 1)){
                Test.TCP.globalMapOfValue[tmp + test_addr] shouldBe Test.TCP.PDStation1.Ctrl.DBRead(6,MdRegion.MD_REGION_4XXXX, test_addr + tmp)
            }
        }
    }
}












