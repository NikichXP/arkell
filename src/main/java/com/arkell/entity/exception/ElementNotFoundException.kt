package com.arkell.entity.exception

import java.io.FileNotFoundException

class ElementNotFoundException(message: String) : FileNotFoundException(message) {
}