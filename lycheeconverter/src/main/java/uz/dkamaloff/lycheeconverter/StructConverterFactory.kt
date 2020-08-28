package uz.dkamaloff.lycheeconverter

import android.util.JsonWriter
import net.aquadc.persistence.android.json.json
import net.aquadc.persistence.android.json.tokens
import net.aquadc.persistence.android.json.writeTo
import net.aquadc.persistence.struct.Schema
import net.aquadc.persistence.struct.Struct
import net.aquadc.persistence.tokens.readAs
import net.aquadc.persistence.tokens.tokensFrom
import net.aquadc.persistence.type.DataType
import net.aquadc.persistence.type.collection
import okhttp3.MediaType
import okhttp3.RequestBody
import okhttp3.ResponseBody
import okio.Buffer
import retrofit2.Call
import retrofit2.Converter
import retrofit2.Retrofit
import retrofit2.http.GET
import java.io.OutputStreamWriter
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.lang.reflect.WildcardType
import java.nio.charset.Charset


class StructConverterFactory(
    private val types: Map<Type, DataType<*>>
) : Converter.Factory() {

    override fun requestBodyConverter(
        type: Type,
        parameterAnnotations: Array<Annotation>,
        methodAnnotations: Array<Annotation>,
        retrofit: Retrofit
    ): Converter<*, RequestBody>? =
        StructRequestBodyConverter(dataTypeOf(type))

    override fun responseBodyConverter(
        type: Type,
        annotations: Array<Annotation>,
        retrofit: Retrofit
    ): Converter<ResponseBody, *>? =
        StructResponseBodyConverter(dataTypeOf(type))

    private fun dataTypeOf(type: Type): DataType<*> = types[type] ?: when (type) {
        is ParameterizedType -> {
            if (type.rawType == List::class.java)
                collection(dataTypeOf(type.actualTypeArguments[0]))
            else if (type.rawType == Struct::class.java)
                (type.actualTypeArguments.single() as Class<*>).getField("INSTANCE").get(null) as DataType<*>
            else
                throw UnsupportedOperationException(type.toString())
        }
        is WildcardType -> {
            dataTypeOf(type.upperBounds.single { it != Any::class.java })
        }
        else -> {
            throw UnsupportedOperationException(type.toString())
        }
    }

    companion object DEFAULT : Converter.Factory() {
        private val delegate = StructConverterFactory(emptyMap())

        override fun responseBodyConverter(
            type: Type, annotations: Array<Annotation>, retrofit: Retrofit
        ): Converter<ResponseBody, *>? =
            delegate.responseBodyConverter(type, annotations, retrofit)

        override fun requestBodyConverter(
            type: Type,
            parameterAnnotations: Array<Annotation>,
            methodAnnotations: Array<Annotation>,
            retrofit: Retrofit
        ): Converter<*, RequestBody>? =
            delegate.requestBodyConverter(type, parameterAnnotations, methodAnnotations, retrofit)

        override fun stringConverter(
            type: Type, annotations: Array<Annotation>, retrofit: Retrofit
        ): Converter<*, String>? =
            delegate.stringConverter(type, annotations, retrofit)
    }
}

private val MEDIA_TYPE: MediaType = MediaType.get("application/json; charset=UTF-8")
private val UTF_8: Charset = Charset.forName("UTF-8")
class StructRequestBodyConverter<T>(private val dataType: DataType<T>) : Converter<T, RequestBody> {
    override fun convert(value: T): RequestBody? {
        val buffer = Buffer()
        JsonWriter(OutputStreamWriter(buffer.outputStream(), UTF_8)).use(dataType.tokensFrom(value)::writeTo)
        return RequestBody.create(MEDIA_TYPE, buffer.readByteString())
    }
}

class StructResponseBodyConverter<T>(private val dataType: DataType<T>) : Converter<ResponseBody, T> {

    override fun convert(value: ResponseBody): T? =
        try {
            value.string().reader().json().tokens().readAs(dataType)
        } catch (e: Exception) {
            println(e)
            throw e
        }
}

object User : Schema<User>()

interface Users {
    @GET("/")
    fun get(): Call<Struct<User>>
}

fun main() {
    Retrofit.Builder()
        .addConverterFactory(StructConverterFactory)
        .baseUrl("http://example.com/")
        .build()
        .create(Users::class.java)
        .get()
}
