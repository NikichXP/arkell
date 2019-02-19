package com.arkell.api

import com.arkell.entity.Promocode
import com.arkell.model.PromocodeService
import com.arkell.model.auth.AuthService
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/promocode")
class PromocodeAPI(
		val promocodeService: PromocodeService,
		val authService: AuthService) {

	@GetMapping("/claimed")
	fun claimed(@RequestHeader token: String): List<Promocode> {
		return promocodeService.getUserCodes(authService.getUser(token))
	}

	/**
	 * Claim a code for offer by id. If offer have public codes - simply returns them. if offer contains only private
	 * codes - locks one of them
	 *
	 * @return 805 if there is no public codes and no token provided; 806 if there is no public and unclaimed codes left.
	 */
	@GetMapping("/claim")
	fun claim(@RequestHeader token: String?, @RequestParam offerId: String): Promocode {
		return promocodeService.getAndClaimCode(offerId, token?.let { authService.getUser(it) })
	}

	/**
	 * Get promocode sent on mail
	 */
	@PostMapping("/send")
	fun send(@RequestParam mail: String, @RequestParam offerId: String): String {
		return promocodeService.sendToMail(mail, offerId)
	}

}