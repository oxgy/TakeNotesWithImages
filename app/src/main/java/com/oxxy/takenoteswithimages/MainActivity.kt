package com.oxxy.takenoteswithimages

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.lifecycle.ReportFragment.Companion.reportFragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.oxxy.takenoteswithimages.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity()  {

    private lateinit var binding: ActivityMainBinding
    private lateinit var noteList: ArrayList<Note>
    private lateinit var noteAdapter: NoteAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate((layoutInflater))
        val view = binding.root
        setContentView(view)

        noteList = ArrayList<Note>()

        noteAdapter = NoteAdapter(noteList)
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = noteAdapter


        try {
            val database = this.openOrCreateDatabase("Notes", MODE_PRIVATE, null)

            val cursor = database.rawQuery("SELECT * FROM notes", null)
            val noteTitleIndex = cursor.getColumnIndex("notetitle")
            val idIndex = cursor.getColumnIndex("id")

            while (cursor.moveToNext()){
                val name = cursor.getString(noteTitleIndex)
                val id = cursor.getInt(idIndex)
                val note = Note(name,id)
                noteList.add(note)
            }

            noteAdapter.notifyDataSetChanged()
            cursor.close()

        }catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {

        val menuInflater = getMenuInflater()
        menuInflater.inflate(R.menu.notes_menu,menu)

        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        if(item.itemId == R.id.add_note_item){
            val intent = Intent(this@MainActivity, NoteActivity::class.java)
            intent.putExtra("info", "new")
            startActivity(intent)
        }


        return super.onOptionsItemSelected(item)
    }


}