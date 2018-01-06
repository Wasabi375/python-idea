/*
 * Copyright (C) 2018 Burkhard Mittelbach
 *
 * This file is part of pythonidea.
 *
 *     pythonidea is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     pythonidea is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with pythonidea.  If not, see <http://www.gnu.org/licenses/>.
 */


package de.wasabi375.pythonidea

import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.command.*
import com.intellij.openapi.diagnostic.*
import com.intellij.openapi.editor.*
import com.intellij.openapi.editor.colors.*
import com.intellij.openapi.project.*
import com.intellij.openapi.ui.popup.*
import com.intellij.ui.layout.*
import javafx.scene.input.*
import org.python.core.*
import org.python.util.*
import java.awt.event.*
import java.awt.event.KeyEvent
import javax.script.*
import javax.swing.*

private class PythonResult(val text: String, val success: Boolean)


class EvalAction : AnAction() {

	override fun actionPerformed(event: AnActionEvent) {

		val project = event.project ?: return
		val editor = event.getData(PlatformDataKeys.EDITOR) ?: return

		val caretModel = editor.caretModel

		if(caretModel.caretCount == 1 && caretModel.primaryCaret.hasSelection()){
			val code = caretModel.primaryCaret.selectedText!!
			val result = runPythonCode(code)

			if(result.success){
				writeResult(project, editor, result.text)
			} else {
				showPythonCodePopup(event, code, result.text)
			}
		} else {
			showPythonCodePopup(event)
		}
	}

	private fun runPythonCode(code: String): PythonResult  = try {
			PythonResult(PythonInterpreter().eval(code).toString(), true)
		} catch (e: PyException){
			PythonResult(e.message ?: "unknown error∂", false)
		}


	private fun writeResult(project: Project, editor: Editor, result: String){


		WriteCommandAction.runWriteCommandAction(project){

			editor.caretModel.allCarets.forEach {
				if(it.hasSelection()){
					editor.document.replaceString(it.selectionStart, it.selectionEnd, result)
				} else {
					editor.document.insertString(it.offset, result)
				}
			}
		}
	}

	private fun showPythonCodePopup(event: AnActionEvent, code: String = "", result: String = ""){

		val project = requireNotNull(event.project)
		val editor = requireNotNull(event.getData(PlatformDataKeys.EDITOR))

		val popupFactory = JBPopupFactory.getInstance()

		val codeArea = JTextArea(code, 10, 70)
		val resultArea = JTextArea(result, 5, 0).apply { isEnabled = false }
		val evalButton = JButton("Eval")
		val acceptButton = JButton("Eval & Accept")

		val panel = panel{
			row{
				codeArea()
			}
			row{
				resultArea()
			}
			row{
				evalButton()
				acceptButton()
			}
		}

		val popup = popupFactory.createComponentPopupBuilder(panel, codeArea).createPopup()

		fun eval(){
			val result = runPythonCode(codeArea.text)
			resultArea.text = result.text
		}

		fun accept(){
			val result = runPythonCode(codeArea.text)

			if(result.success){
				writeResult(project, editor, result.text)

				popup.closeOk(null)
			} else {
				resultArea.text = result.text
			}
		}

		codeArea.addKeyListener(object: KeyListener{
			override fun keyTyped(e: KeyEvent) {}

			override fun keyPressed(e: KeyEvent) {}

			override fun keyReleased(e: KeyEvent){
				if(e.keyCode == KeyEvent.VK_ENTER && (e.isMetaDown || e.isControlDown)){

					if(e.isShiftDown)
						accept()
					else
						eval()
				}
			}
		})

		evalButton.addActionListener{ eval() }

		acceptButton.addActionListener{ accept() }

		popup.showInBestPositionFor(editor)
		codeArea.grabFocus()
	}

	override fun update(event: AnActionEvent?) {

		if(event == null) return
		if(event.project == null) return

		event.presentation.isEnabled = false

		val editor = event.getData(PlatformDataKeys.EDITOR) ?: return

		event.presentation.isEnabled = editor.document.isWritable
	}
}
