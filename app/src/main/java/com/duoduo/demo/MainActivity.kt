package com.duoduo.demo

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import com.duoduo.objectstore.ObjectStore

class MainActivity : AppCompatActivity() {

    val readContent: TextView by lazy { findViewById(R.id.readContent) }
    val writeObj1: Button by lazy { findViewById(R.id.writeOb1) }
    val readObj1: Button by lazy { findViewById(R.id.readObj1) }


    val obj1Store: ObjectStore<TestObj> by lazy { ObjectStore(this, "testObj") }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setListeners()
    }


    fun setListeners() {
        writeObj1.setOnClickListener {
            TestObj(
                name = "钟华健",
                age = 32,
                sex = true,
                address = "中国香港"
            ).also {
                obj1Store.write(it)
            }
        }

        readObj1.setOnClickListener {
            obj1Store.read {
                readContent.text = "${it}"
            }
        }
    }


}