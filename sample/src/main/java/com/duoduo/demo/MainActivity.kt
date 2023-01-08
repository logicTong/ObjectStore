package com.duoduo.demo

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import com.duoduo.objectstore.ObjectStore

class MainActivity : AppCompatActivity() {

    private val readContent: TextView by lazy { findViewById(R.id.readContent) }
    private val writeObj1: Button by lazy { findViewById(R.id.writeOb1) }
    private val readObj1: Button by lazy { findViewById(R.id.readObj1) }
    private val writeList: Button by lazy { findViewById(R.id.writeList) }
    private val readList: Button by lazy { findViewById(R.id.readList) }
    private val deleteFile: Button by lazy { findViewById(R.id.deleteFile) }
    private val waitFinish: Button by lazy { findViewById(R.id.waitFinish) }


    private val objStore: ObjectStore<TestObj> by lazy {
        ObjectStore(this, "testObj")
    }
    private val listStore: ObjectStore<ArrayList<TestObj>> by lazy {
        ObjectStore(
            this,
            "listTestObj"
        )
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setListeners()
    }


    private fun setListeners() {
        writeObj1.setOnClickListener {
            TestObj(
                name = "钟华健",
                age = 32,
                sex = true,
                address = "中国香港"
            ).also {
                objStore.write(it)
            }
        }

        readObj1.setOnClickListener {
            objStore.read(true) {
                readContent.text = "${it}"
            }
        }


        writeList.setOnClickListener {

            ArrayList<TestObj>().apply {
                add(
                    TestObj(
                        name = "刘德华",
                        age = 60,
                        sex = true,
                        address = "中国香港"
                    )
                )
                add(
                    TestObj(
                        name = "赵丽颖",
                        age = 34,
                        sex = false,
                        address = "中国大陆"
                    )
                )
                add(
                    TestObj(
                        name = "权志龙",
                        age = 24,
                        sex = true,
                        address = "korean"
                    )
                )
            }.let {
                listStore.write(it)
            }
        }

        readList.setOnClickListener {
            listStore.read().observe(this) { list ->
                list?.let {
                    readContent.text = "${it}"
                }
            }
        }

        deleteFile.setOnClickListener {
            objStore.clear()
            listStore.clear()
            objStore.read(true) {
                readContent.text = "$it"
            }
        }

        waitFinish.setOnClickListener {
            listStore.waitWriteFinish()
        }

    }


}