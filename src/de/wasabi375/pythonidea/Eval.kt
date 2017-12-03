package de.wasabi375.pythonidea

import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.command.*
import com.intellij.openapi.diagnostic.*
import com.intellij.openapi.vfs.*

class Eval : AnAction() {

	companion object {
		private val log = Logger.getInstance(Eval::class.java)

		private val alwaysLog = true

		fun Eval.log(message: () -> String){
			if(log.isDebugEnabled || alwaysLog){
				log.info(message())
			}
		}

		fun Eval.warn(message: () -> String){
			if(log.isDebugEnabled || alwaysLog){
				log.warn(message())
			}
		}

		fun Eval.error(t: Throwable) = log.error(t)
		fun Eval.error(message: () -> String) = log.error(message())
	}

	override fun actionPerformed(event: AnActionEvent) {

		val project = event.project ?: return
		val editor = event.getData(PlatformDataKeys.EDITOR) ?: return

		val caretModel = editor.caretModel


		val pythonCode =
				if(caretModel.caretCount == 1) { caretModel.primaryCaret.selectedText }
				else { null }
			?: getPythonCode(event)

		WriteCommandAction.runWriteCommandAction(project) {

			caretModel.allCarets.forEach{

				if(it.hasSelection()){
					editor.document.replaceString(it.selectionStart, it.selectionEnd, pythonCode)
				} else {
					editor.document.insertString(it.offset, pythonCode)
				}
			}
		}


	}

	fun getPythonCode(event: AnActionEvent) : String{
		return ""
	}

	override fun update(event: AnActionEvent?) {

		if(event == null) return
		if(event.project == null) return

		event.presentation.isEnabled = false

		val editor = event.getData(PlatformDataKeys.EDITOR) ?: return

		event.presentation.isEnabled = editor.document.isWritable
	}
}
