package com.absinthe.anywhere_.model.database

import android.os.Parcelable
import android.provider.BaseColumns
import androidx.annotation.Keep
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.absinthe.anywhere_.constants.AnywhereType
import com.absinthe.anywhere_.constants.GlobalValues
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

@Keep
@Parcelize
@Entity(tableName = "anywhere_table")
data class AnywhereEntity(
    @PrimaryKey
    @ColumnInfo(name = BaseColumns._ID)
    var id: String = System.currentTimeMillis().toString(),

    @ColumnInfo(name = APP_NAME)
    var appName: String = "",

    @ColumnInfo(name = PARAM_1)
    var param1: String = "",

    @ColumnInfo(name = PARAM_2)
    var param2: String? = "",

    @ColumnInfo(name = PARAM_3)
    var param3: String? = "",

    @ColumnInfo(name = DESCRIPTION)
    var description: String? = "",

    @ColumnInfo(name = TYPE)
    var type: Int = AnywhereType.Card.NOT_CARD,

    @ColumnInfo(name = CATEGORY)
    var category: String? = GlobalValues.category,

    @ColumnInfo(name = TIME_STAMP)
    var timeStamp: String = id,

    @ColumnInfo(name = COLOR)
    var color: Int = 0,

    @ColumnInfo(name = ICON_URI)
    var iconUri: String? = "",

    @ColumnInfo(name = EXEC_WITH_ROOT)
    var execWithRoot: Boolean = false,
) : Parcelable {

    @IgnoredOnParcel
    val packageName: String
        get() = when (type) {
            AnywhereType.Card.URL_SCHEME -> param2.orEmpty()
            AnywhereType.Card.ACTIVITY, AnywhereType.Card.QR_CODE -> param1
            else -> ""
        }

    companion object {

        const val APP_NAME = "app_name"
        const val PARAM_1 = "param_1"
        const val PARAM_2 = "param_2"
        const val PARAM_3 = "param_3"
        const val DESCRIPTION = "description"
        const val TYPE = "type"
        const val CATEGORY = "category"
        const val TIME_STAMP = "time_stamp"
        const val COLOR = "color"
        const val ICON_URI = "iconUri"
        const val EXEC_WITH_ROOT = "execWithRoot"

        fun getClearedEntity(entity: AnywhereEntity) = entity.apply {
            category = ""
            iconUri = ""
        }
    }
}