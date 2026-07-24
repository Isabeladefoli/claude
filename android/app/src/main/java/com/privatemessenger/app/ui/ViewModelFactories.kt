package com.privatemessenger.app.ui

import android.content.Context
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.privatemessenger.app.data.MessengerRepository
import com.privatemessenger.app.vm.AuthViewModel
import com.privatemessenger.app.vm.ChatViewModel
import com.privatemessenger.app.vm.ConversationsViewModel

// Os ViewModels precisam do repositório pra funcionar. Estas "fábricas" ensinam
// o Compose a construí-los passando o repositório. É um detalhe técnico do
// Android; o importante é que cada tela pega seu ViewModel já pronto.

fun authViewModelFactory(repo: MessengerRepository, context: Context) = viewModelFactory {
    initializer { AuthViewModel(repo, context) }
}

fun conversationsViewModelFactory(repo: MessengerRepository) = viewModelFactory {
    initializer { ConversationsViewModel(repo) }
}

fun chatViewModelFactory(repo: MessengerRepository, partnerId: Long) = viewModelFactory {
    initializer { ChatViewModel(repo, partnerId) }
}
