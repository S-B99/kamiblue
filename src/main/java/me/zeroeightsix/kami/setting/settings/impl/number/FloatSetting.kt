package me.zeroeightsix.kami.setting.settings.impl.number

import com.google.gson.JsonElement

class FloatSetting(
        name: String,
        value: Float,
        range: ClosedFloatingPointRange<Float>,
        step: Float,
        visibility: () -> Boolean = { true },
        consumer: (prev: Float, input: Float) -> Float = { _, input -> input },
        description: String = ""
) : NumberSetting<Float>(name, value, range.start, range.endInclusive, step, visibility, consumer, description) {

    override var value: Float = value
        set(value) {
            field = consumer(field, value.coerceIn(min, max))
            for (listener in listeners) listener()
        }

    override fun read(jsonElement: JsonElement?) {
        jsonElement?.asJsonPrimitive?.asFloat?.let { value = it }
    }

}