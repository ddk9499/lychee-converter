package uz.dkamaloff.lycheeconverter

import net.aquadc.persistence.android.json.json
import net.aquadc.persistence.android.json.tokens
import net.aquadc.persistence.tokens.readListOf
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Converter
import retrofit2.Retrofit
import java.lang.reflect.Type

class StructConverterFactory : Converter.Factory() {

    override fun requestBodyConverter(
        type: Type,
        parameterAnnotations: Array<Annotation>,
        methodAnnotations: Array<Annotation>,
        retrofit: Retrofit
    ): Converter<*, RequestBody>? {
        return super.requestBodyConverter(type, parameterAnnotations, methodAnnotations, retrofit)
    }

    override fun responseBodyConverter(
        type: Type,
        annotations: Array<Annotation>,
        retrofit: Retrofit
    ): Converter<ResponseBody, *>? {
        return super.responseBodyConverter(type, annotations, retrofit)
    }
}

class StructRequestBodyConverter<T> : Converter<T, RequestBody> {
    override fun convert(value: T): RequestBody? {
        TODO("Not yet implemented")
    }
}

class StructResponseBodyConverter<T> : Converter<ResponseBody, T> {

    override fun convert(value: ResponseBody): T? {
        val json = value.string()
        try {
            return json.reader().json().tokens().readListOf()
        } catch (e: Exception) {
            println(e)
            throw e
        }
    }
}