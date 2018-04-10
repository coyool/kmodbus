package Test.TCP

import Base.toSimpleHex
import KModbus.*
import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec


class TCP_MASTER_RD : StringSpec() {
    init {
        val PDBus = StationBus(
                RawPkgLen = 0,
                RawBytes  = ByteArray(256),
                RspPkgLen = 0,
                Stations  = listOf(PDStation1, PDStation2)
        )
        val polling = Polling(
                StationID  = 0x17,
                StartAddr  = 0x1123,
                NumbTodo = 12,
                DevCmd     = MdCmdEnum.RD_WORD
        )
        PDBus.Serial = 0x3344
        PDBus.NowPoll.polling = polling
        PDBus.NowPoll.station = PDStation1
        
        val tmp1 = KM_TCP_Master_Setup(
                PDBus,PDBus.NowPoll
        )
        "TCP Master RD_WORD" {
            tmp1.toSimpleHex() shouldBe "33440000000617031123000C"
        }
    }
}
class TCP_MASTER_COIL_RD : StringSpec() {
    init {
        val PDBus = StationBus(
                RawPkgLen = 0,
                RawBytes  = ByteArray(256),
                RspPkgLen = 0,
                Stations  = listOf(PDStation1, PDStation2)
        )
        val polling = Polling(
                StationID  = 0x17,
                StartAddr  = 0x1123,
                NumbTodo = 12,
                DevCmd     = MdCmdEnum.RD_BITS
        )
        PDBus.Serial = 0x3344
        PDBus.NowPoll.polling = polling
        PDBus.NowPoll.station = PDStation1

        val tmp1 = KM_TCP_Master_Setup(
                PDBus,PDBus.NowPoll
        )
        "TCP Master RD_BITS" {
            tmp1.toSimpleHex() shouldBe "33440000000617011123000C"
        }
    }
}

class TCP_MASTER_BIT_RD : StringSpec() {
    init {
        val PDBus = StationBus(
                RawPkgLen = 0,
                RawBytes  = ByteArray(256),
                RspPkgLen = 0,
                Stations  = listOf(PDStation1, PDStation2)
        )
        val polling = Polling(
                StationID  = 0x17,
                StartAddr  = 0x1123,
                NumbTodo = 12,
                DevCmd     = MdCmdEnum.RD_BITS_RO
        )
        PDBus.Serial = 0x3344
        PDBus.NowPoll.polling = polling
        PDBus.NowPoll.station = PDStation1

        val tmp1 = KM_TCP_Master_Setup(
                PDBus,PDBus.NowPoll
        )
        "TCP Master RD_BITS_RO" {
            tmp1.toSimpleHex() shouldBe "33440000000617021123000C"
        }
    }
}

class TCP_MASTER_RDO : StringSpec() {
    init {
        val PDBus = StationBus(
                RawPkgLen = 0,
                RawBytes  = ByteArray(256),
                RspPkgLen = 0,
                Stations  = listOf(PDStation1, PDStation2)
        )
        val polling = Polling(
                StationID  = 0x17,
                StartAddr  = 0x1123,
                NumbTodo = 12,
                DevCmd     = MdCmdEnum.RD_WORD_RO
        )
        PDBus.Serial = 0x3344
        PDBus.NowPoll.polling = polling
        PDBus.NowPoll.station = PDStation1

        val tmp1: ByteArray = KM_TCP_Master_Setup(
                PDBus,PDBus.NowPoll
        )
        "TCP Master RD_WORD_RO" {
            tmp1.toSimpleHex() shouldBe "33440000000617041123000C"
        }
    }
}

class TCP_MASTER_WRC : StringSpec() {
    init {
        val PDBus = StationBus(
                RawPkgLen = 0,
                RawBytes  = ByteArray(256),
                RspPkgLen = 0,
                Stations  = listOf(PDStation1, PDStation2)
        )
        val polling = Polling(
                StationID  = 0x17,
                StartAddr  = 0x1123,
                NumbTodo   = 1,
                DevCmd     = MdCmdEnum.WR_BITS_ONE
        )
        PDBus.Serial = 0x3344
        PDBus.NowPoll.polling = polling
        PDBus.NowPoll.station = PDStation1

        val tmp1 = KM_TCP_Master_Setup(
                PDBus,PDBus.NowPoll
        )
        "TCP Master WR_BITS_ONE" {
            tmp1.toSimpleHex() shouldBe "33440000000617051123FF00"
        }
    }
}

class TCP_MASTER_WRCP : StringSpec() {
    init {
        val PDBus = StationBus(
                RawPkgLen = 0,
                RawBytes  = ByteArray(256),
                RspPkgLen = 0,
                Stations  = listOf(PDStation1, PDStation2)
        )
        val polling = Polling(
                StationID  = 0x17,
                StartAddr  = 0x1123,
                NumbTodo   = 12,
                DevCmd     = MdCmdEnum.WR_BITS_SOME
        )
        PDBus.Serial = 0x3344
        PDBus.NowPoll.polling = polling
        PDBus.NowPoll.station = PDStation1

        val tmp1 = KM_TCP_Master_Setup(
                PDBus,PDBus.NowPoll
        )
        "TCP Master WR_BITS_SOME" {
            tmp1.toSimpleHex() shouldBe "334400000009170F1123000C025505"
        }
    }
}


class TCP_MASTER_WRP : StringSpec() {
    init {
        val PDBus = StationBus(
                RawPkgLen = 0,
                RawBytes  = ByteArray(256),
                RspPkgLen = 0,
                Stations  = listOf(PDStation1, PDStation2)
        )
        val polling = Polling(
                StationID  = 0x17,
                StartAddr  = 0x1123,
                NumbTodo   = 12,
                DevCmd     = MdCmdEnum.WR_WORD_SOME
        )
        PDBus.Serial = 0x3344
        PDBus.NowPoll.polling = polling
        PDBus.NowPoll.station = PDStation1

        val tmp1 = KM_TCP_Master_Setup(
                PDBus,PDBus.NowPoll
        )
        "TCP Master WR_WORD_SOME" {
            tmp1.toSimpleHex() shouldBe "33440000001F17101123000C18073B073C073D073E073F0740074107420743074407450746"
        }
    }
}





