package Test.TCP

import Base.getBit
import Base.getInt
import Base.toSimpleHex
import Base.toUInt
import KModbus.*
import Test.RTU.PDCtrls1
import Test.RTU.PDCtrls2
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
        globalMapOfValue.put(addr,data.toUInt())
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
        globalMapOfValue.put(addr,data.toUInt())
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



class TEST_MD_CMD_COIL_RD: StringSpec() {
    init {
        val test_station_id = 10
        val test_start_addr = 0x1123
        val test_numb_todo  = 12
        val PDBus = StationBus(
                RawPkgLen = 0,
                RawBytes  = ByteArray(256),
                RspPkgLen = 0,
                Stations  = listOf(PDStation1, PDStation2)
        )
        val polling = Polling(
                StationID  = test_station_id,
                StartAddr  = test_start_addr,
                NumbTodo   = test_numb_todo,
                DevCmd     = MdCmdEnum.RD_BITS
        )
        PDBus.Serial          = 0x3344
        PDBus.NowPoll.polling = polling
        PDBus.NowPoll.station = PDStation1

        PDBus.RawBytes = KM_TCP_Master_Setup(
                PDBus,PDBus.NowPoll
        )
        PDBus.RawPkgLen = PDBus.RawBytes.size

        "RawPkgLen Check"{
            PDBus.RawPkgLen shouldBe 12
        }

        "TEST RD_BITS" {
            val ret = KM_TCP_Data_Deal(PDBus)
            ret.code shouldBe MdRspCode.MD_ERR_OK
            ret.rsp.toSimpleHex() shouldBe "3344000000050A01025505"
            for (tmp in 0..(test_numb_todo - 1)){
                ret.rsp.getBit(tmp + TCP_RSP_DATA_INDEX*8) shouldBe
                        PDStation1.Ctrl.DBRead(test_station_id,MdRegion.MD_REGION_1XXXX, test_start_addr + tmp)
            }
        }
    }
}


class TEST_RD_BITS_RO: StringSpec() {
    init {
        val test_station_id = 10
        val test_start_addr = 0x1122
        val test_numb_todo  = 12
        val PDBus = StationBus(
                RawPkgLen = 0,
                RawBytes  = ByteArray(256),
                RspPkgLen = 0,
                Stations  = listOf(PDStation1, PDStation2)
        )
        val polling = Polling(
                StationID  = test_station_id,
                StartAddr  = test_start_addr,
                NumbTodo   = test_numb_todo,
                DevCmd     = MdCmdEnum.RD_BITS_RO
        )
        PDBus.Serial          = 0x3344
        PDBus.NowPoll.polling = polling
        PDBus.NowPoll.station = PDStation1

        PDBus.RawBytes = KM_TCP_Master_Setup(
                PDBus,PDBus.NowPoll
        )
        PDBus.RawPkgLen = PDBus.RawBytes.size

        "RawPkgLen Check"{
            PDBus.RawPkgLen shouldBe 12
        }

        "TEST RD_BITS_RO" {
            val ret = KM_TCP_Data_Deal(PDBus)
            ret.code shouldBe MdRspCode.MD_ERR_OK
            ret.rsp.toSimpleHex() shouldBe "3344000000050A0202AA0A"
            for (tmp in 0..(test_numb_todo - 1)){
                ret.rsp.getBit(tmp + TCP_RSP_DATA_INDEX*8) shouldBe
                        PDStation1.Ctrl.DBRead(test_station_id,MdRegion.MD_REGION_2XXXX, test_start_addr + tmp)
            }
        }
    }
}


class TEST_RD_WORD_RO: StringSpec() {
    init {
        val test_station_id = 10
        val test_start_addr = 0x1152
        val test_numb_todo  = 12
        val PDBus = StationBus(
                RawPkgLen = 0,
                RawBytes  = ByteArray(256),
                RspPkgLen = 0,
                Stations  = listOf(PDStation1, PDStation2)
        )
        val polling = Polling(
                StationID  = test_station_id,
                StartAddr  = test_start_addr,
                NumbTodo   = test_numb_todo,
                DevCmd     = MdCmdEnum.RD_WORD_RO
        )
        PDBus.Serial          = 0x3366
        PDBus.NowPoll.polling = polling
        PDBus.NowPoll.station = PDStation1

        PDBus.RawBytes = KM_TCP_Master_Setup(
                PDBus,PDBus.NowPoll
        )
        PDBus.RawPkgLen = PDBus.RawBytes.size
        println(PDBus.RawBytes.toSimpleHex()+"////")

        "RawPkgLen Check"{
            PDBus.RawPkgLen shouldBe 12
        }

        "TEST RD_WORD_RO" {
            val ret = KM_TCP_Data_Deal(PDBus)
            ret.code shouldBe MdRspCode.MD_ERR_OK
            ret.rsp.toSimpleHex() shouldBe "33660000001B0A0418AD92AD93AD94AD95AD96AD97AD98AD99AD9AAD9BAD9CAD9D"
            for (tmp in 0..(test_numb_todo - 1)){
                ret.rsp.getInt(tmp*2 + TCP_RSP_DATA_INDEX) shouldBe
                        PDStation1.Ctrl.DBRead(test_station_id,MdRegion.MD_REGION_3XXXX, test_start_addr + tmp)
            }
        }
    }
}


class TEST_RD_WORD: StringSpec() {
    init {
        val test_station_id = 10
        val test_start_addr = 0x1152
        val test_numb_todo  = 12
        val PDBus = StationBus(
                RawPkgLen = 0,
                RawBytes  = ByteArray(256),
                RspPkgLen = 0,
                Stations  = listOf(PDStation1, PDStation2)
        )
        val polling = Polling(
                StationID  = test_station_id,
                StartAddr  = test_start_addr,
                NumbTodo   = test_numb_todo,
                DevCmd     = MdCmdEnum.RD_WORD
        )
        PDBus.Serial          = 0x3366
        PDBus.NowPoll.polling = polling
        PDBus.NowPoll.station = PDStation1

        PDBus.RawBytes = KM_TCP_Master_Setup(
                PDBus,PDBus.NowPoll
        )
        PDBus.RawPkgLen = PDBus.RawBytes.size

        "RawPkgLen Check"{
            PDBus.RawPkgLen shouldBe 12
        }

        "TEST RD_WORD" {
            val ret = KM_TCP_Data_Deal(PDBus)
            ret.code shouldBe MdRspCode.MD_ERR_OK
            ret.rsp.toSimpleHex() shouldBe "33660000001B0A0318D4A2D4A3D4A4D4A5D4A6D4A7D4A8D4A9D4AAD4ABD4ACD4AD"
            for (tmp in 0..(test_numb_todo - 1)){
                ret.rsp.getInt(tmp*2 + TCP_RSP_DATA_INDEX) shouldBe
                        PDStation1.Ctrl.DBRead(test_station_id,MdRegion.MD_REGION_4XXXX, test_start_addr + tmp)
            }
        }
    }
}



class TEST_WR_WORD_ONE : StringSpec() {
    init {
        val test_station_id = 10
        val test_start_addr = 0x1152
        val test_numb_todo  = 12
        val PDBus = StationBus(
                RawPkgLen = 0,
                RawBytes  = ByteArray(256),
                RspPkgLen = 0,
                Stations  = listOf(PDStation1, PDStation2)
        )
        val polling = Polling(
                StationID  = test_station_id,
                StartAddr  = test_start_addr,
                NumbTodo   = test_numb_todo,
                DevCmd     = MdCmdEnum.WR_WORD_ONE
        )
        PDBus.Serial          = 0x3366
        PDBus.NowPoll.polling = polling
        PDBus.NowPoll.station = PDStation1

        PDBus.RawBytes = KM_TCP_Master_Setup(
                PDBus,PDBus.NowPoll
        )
        PDBus.RawPkgLen = PDBus.RawBytes.size

        "RawPkgLen Check"{
            PDBus.RawPkgLen shouldBe 12
        }

        "TEST WR_WORD_ONE" {
            val ret = KM_TCP_Data_Deal(PDBus)
            ret.code shouldBe MdRspCode.MD_ERR_OK
            ret.rsp.toSimpleHex() shouldBe "3366000000060A061152D4A2"
            ret.rsp.getInt(TCP_REQ_SINGLE_DATA_INDEX) shouldBe
                    PDStation1.Ctrl.DBRead(test_station_id,MdRegion.MD_REGION_4XXXX, test_start_addr)
        }
    }
}



class TEST_WR_WORD_SOME : StringSpec() {
    init {
        val test_station_id = 10
        val test_start_addr = 0x1152
        val test_numb_todo  = 12
        val PDBus = StationBus(
                RawPkgLen = 0,
                RawBytes  = ByteArray(256),
                RspPkgLen = 0,
                Stations  = listOf(PDStation1, PDStation2)
        )
        val polling = Polling(
                StationID  = test_station_id,
                StartAddr  = test_start_addr,
                NumbTodo   = test_numb_todo,
                DevCmd     = MdCmdEnum.WR_WORD_SOME
        )
        PDBus.Serial          = 0x3366
        PDBus.NowPoll.polling = polling
        PDBus.NowPoll.station = PDStation1

        PDBus.RawBytes = KM_TCP_Master_Setup(
                PDBus,PDBus.NowPoll
        )
        PDBus.RawPkgLen = PDBus.RawBytes.size

        "RawPkgLen Check"{
            PDBus.RawPkgLen shouldBe (TCP_HEADER_LEN+TCP_REQ_WRP_EXTRA_BYTES+test_numb_todo*2)
        }

        "TEST WR_WORD_SOME" {
            val ret = KM_TCP_Data_Deal(PDBus)
            ret.code shouldBe MdRspCode.MD_ERR_OK
            ret.rsp.toSimpleHex() shouldBe "3366000000060A101152000C"
            for (tmp in 0..(test_numb_todo - 1)){
                globalMapOfValue[tmp + test_start_addr] shouldBe
                       PDStation1.Ctrl.DBRead(test_station_id,MdRegion.MD_REGION_4XXXX, test_start_addr + tmp)
            }
        }
    }
}




class TEST_WR_BITS_ONE: StringSpec() {
    init {
        val test_station_id = 10
        val test_start_addr = 0x1152
        val test_numb_todo  = 12
        val PDBus = StationBus(
                RawPkgLen = 0,
                RawBytes  = ByteArray(256),
                RspPkgLen = 0,
                Stations  = listOf(PDStation1, PDStation2)
        )
        val polling = Polling(
                StationID  = test_station_id,
                StartAddr  = test_start_addr,
                NumbTodo   = test_numb_todo,
                DevCmd     = MdCmdEnum.WR_BITS_ONE
        )
        PDBus.Serial          = 0x3366
        PDBus.NowPoll.polling = polling
        PDBus.NowPoll.station = PDStation1

        PDBus.RawBytes = KM_TCP_Master_Setup(
                PDBus,PDBus.NowPoll
        )
        PDBus.RawPkgLen = PDBus.RawBytes.size

        "RawPkgLen Check"{
            PDBus.RawPkgLen shouldBe 12
        }

        "TEST WR_BITS_ONE" {
            val ret = KM_TCP_Data_Deal(PDBus)
            ret.code shouldBe MdRspCode.MD_ERR_OK
            ret.rsp.toSimpleHex() shouldBe "3366000000060A0511520000"
            ret.rsp.getBit(TCP_REQ_SINGLE_DATA_INDEX*8) shouldBe
                    PDStation1.Ctrl.DBRead(test_station_id,MdRegion.MD_REGION_1XXXX, test_start_addr)
        }
    }
}


class TEST_WR_BITS_SOME : StringSpec() {
    init {
        val test_station_id = 10
        val test_start_addr = 0x11FF
        val test_numb_todo  = 120
        val PDBus = StationBus(
                RawPkgLen = 0,
                RawBytes  = ByteArray(256),
                RspPkgLen = 0,
                Stations  = listOf(PDStation1, PDStation2)
        )
        val polling = Polling(
                StationID  = test_station_id,
                StartAddr  = test_start_addr,
                NumbTodo   = test_numb_todo,
                DevCmd     = MdCmdEnum.WR_BITS_SOME
        )
        PDBus.Serial          = 0x3366
        PDBus.NowPoll.polling = polling
        PDBus.NowPoll.station = PDStation1

        PDBus.RawBytes = KM_TCP_Master_Setup(
                PDBus,PDBus.NowPoll
        )
        PDBus.RawPkgLen = PDBus.RawBytes.size

        "RawPkgLen Check"{
            PDBus.RawPkgLen shouldBe (TCP_HEADER_LEN+TCP_REQ_WRP_EXTRA_BYTES+(test_numb_todo+7)/8)
        }

        "TEST WR_BITS_SOME" {
            val ret = KM_TCP_Data_Deal(PDBus)
            ret.code shouldBe MdRspCode.MD_ERR_OK
            ret.rsp.toSimpleHex() shouldBe "3366000000060A0F11FF0078"
            for (tmp in 0..(test_numb_todo - 1)){
                globalMapOfValue[tmp + test_start_addr] shouldBe
                        PDStation1.Ctrl.DBRead(test_station_id,MdRegion.MD_REGION_1XXXX, test_start_addr + tmp)
            }
        }
    }
}




