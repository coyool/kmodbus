package KModbus


const val MODBUS_VERSION             = 1


const val TCP_SERIAL_INDEX           =  0
const val TCP_HEADER_LEN             =  6 //RAW_BYTES = HEADER_LEN + PKG_LEN
const val TCP_PKG_LEN_INDEX          =  4 //ALWAYS for all cmd
const val TCP_STATION_NUMB_INDEX     =  6
const val TCP_PKG_CMD_INDEX          =  7
const val TCP_RSP_RD_DATA_EXTRA_BYTE =  3
const val TCP_REQ_TRG_ADDR_INDEX     =  8
const val TCP_REQ_TRG_NUMB_INDEX     = 10
const val TCP_REQ_SINGLE_DATA_INDEX  = 10
const val TCP_REQ_TRG_DATA_INDEX     = 13
const val TCP_RSP_DATA_INDEX         =  9
const val TCP_RSP_REQ_SHORT_PKG_LEN  =  6
const val TCP_REQ_WRP_EXTRA_BYTES    =  7


const val RTU_SHORT_CMD_LEN          = 0x08
const val RTU_RSP_RD_DATA_INDEX      = 0x03
const val RTU_STATION_NUMB_INDEX     = 0x00
const val RTU_PKG_CMD_INDEX          = 0x01
const val RTU_RSP_BYTE_LEN_INDEX     = 0x02          //回复的字节数所在
const val RTU_TRG_ADDR_INDEX         = 2
const val RTU_REQ_SINGLE_DATA_INDEX  = 4
const val RTU_REQ_WRP_TRG_NUMB_INDEX = 4
const val RTU_REQ_WRP_TRG_DATA_INDEX = 7
const val RTU_REQ_RD_TRG_NUMB_INDEX  = 4
const val RTU_REQ_WRP_BYTE_LEN_INDEX = 6          //批量写的字节数所在位置
const val RTU_REQ_WRP_EXTRA_BYTE     = 9

enum class MdCmdEnum{
    RD_BITS,        //MD_REGION_1XXXX  0x01
    RD_BITS_RO,     //MD_REGION_2XXXX  0x02
    RD_WORD_RO,     //MD_REGION_3XXXX  0x04
    RD_WORD,        //MD_REGION_4XXXX  0x03
    WR_BITS_ONE,    //MD_REGION_1XXXX  0x05
    WR_BITS_SOME,   //MD_REGION_1XXXX  0x0F
    WR_WORD_ONE,    //MD_REGION_3XXXX  0x06
    WR_WORD_SOME,   //MD_REGION_3XXXX  0x10
    UN_SUPPORTED;


    companion object{
        fun parse(code: Byte):MdCmdEnum{
            return when(code.toInt()){
                0x01   -> RD_BITS
                0x05   -> WR_BITS_ONE
                0x0F   -> WR_BITS_SOME
                0x02   -> RD_BITS_RO
                0x04   -> RD_WORD_RO
                0x03   -> RD_WORD
                0x06   -> WR_WORD_ONE
                0x10   -> WR_WORD_SOME
                else   -> UN_SUPPORTED
            }
        }
    }
}


fun MdCmdEnum.toCode(): Int{
    return when(this){
        MdCmdEnum.RD_BITS      ->  0x01
        MdCmdEnum.WR_BITS_ONE  ->  0x05
        MdCmdEnum.WR_BITS_SOME ->  0x0F
        MdCmdEnum.RD_BITS_RO   ->  0x02
        MdCmdEnum.RD_WORD_RO   ->  0x04
        MdCmdEnum.RD_WORD      ->  0x03
        MdCmdEnum.WR_WORD_ONE  ->  0x06
        MdCmdEnum.WR_WORD_SOME ->  0x10
        else                   ->  0xFF
    }

}

val MdSupportCmd = listOf(
        0x01,
        0x05,
        0x0F,
        0x02,
        0x04,
        0x03,
        0x06,
        0x10
)

enum class MdRegion{
    MD_REGION_1XXXX,
    MD_REGION_2XXXX,
    MD_REGION_3XXXX,
    MD_REGION_4XXXX,
}


fun MdRegion.toCode(): Int{
    return when(this){
        MdRegion.MD_REGION_1XXXX ->  0x01
        MdRegion.MD_REGION_2XXXX ->  0x02
        MdRegion.MD_REGION_3XXXX ->  0x03
        MdRegion.MD_REGION_4XXXX ->  0x04 //holding
    }
}

fun MdCmdEnum.toRegion(): MdRegion{
    return when(this){
        MdCmdEnum.RD_BITS      -> MdRegion.MD_REGION_1XXXX
        MdCmdEnum.WR_BITS_ONE  -> MdRegion.MD_REGION_1XXXX
        MdCmdEnum.WR_BITS_SOME -> MdRegion.MD_REGION_1XXXX
        MdCmdEnum.RD_BITS_RO   -> MdRegion.MD_REGION_2XXXX
        MdCmdEnum.RD_WORD_RO   -> MdRegion.MD_REGION_3XXXX
        MdCmdEnum.RD_WORD      -> MdRegion.MD_REGION_4XXXX
        MdCmdEnum.WR_WORD_ONE  -> MdRegion.MD_REGION_4XXXX
        MdCmdEnum.WR_WORD_SOME -> MdRegion.MD_REGION_4XXXX
        MdCmdEnum.UN_SUPPORTED -> MdRegion.MD_REGION_4XXXX
    }
}

enum class MdRspCode
{
    MD_ERR_OK,
    MD_CMD_ERR,
    MD_CRC_ERR,
    MD_FIND_OK,
    MD_POLL_OK,
    MD_LEN_ERR,
    MD_POLL_ERR,
    MD_FIND_NEXT,
    MD_NULL_ERROR,
    MD_CMD_LEN_ERR,
    MD_MEM_REQ_ERR,
    MD_TRG_LEN_ERR,
    MD_CMD_ADDR_ERR,
    MD_CMD_TYPE_ERR,
    MD_DEV_ADDR_ERR,
    MD_TRG_ADDR_ERR,
    MD_ERR_RSP_ID_ERR,
    MD_ERR_RSP_LEN_ERR,
    MD_BUS_DEV_FIND_OK,
    MD_TRG_LEN_LMT_ERR,
    MD_MEM_POLL_NUM_ERR,
    MD_NULL_POINTER_ERR,
    MD_SPE_POLL_IN_DEAL,
    MD_NOT_MASTER_ERROR,
    MD_NOTHING_ENABLE_ERR,
    MD_READ_TOTAL_LEN_ERR,
    MD_WRITE_TOTAL_LEN_ERR,
    MD_ERR_RSP_DATA_LEN_ERR,
    MD_NULL_POINTER_BINDDEV,
    MD_NULL_POINTER_BINDPCB,
    MD_CMD_RESPONSE_FLAG_ERR,
    MD_READING_MAX_LEN_EXCEED,
}











