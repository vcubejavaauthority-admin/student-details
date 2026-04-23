package com.Vcube_AdminPortal.studentDetails.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.Vcube_AdminPortal.studentDetails.excelSheetCodes.OnlineEmailsApp;

@Controller
public class OnlineEmailsController {
	
	@Autowired
	private OnlineEmailsApp online;
	
	@GetMapping("/online/form")
	public String onlineForm() {
	    return "online-emails"; // above HTML
	}
	
	@PostMapping("/online/update-email")
	public String updateOnlineEmail(
	        @RequestParam String email,
	        @RequestParam String date,
	        @RequestParam String batchNo,
	        RedirectAttributes ra) {

	    boolean ok = online.upsertOnlineEmail(batchNo.trim(),email.trim(), date.trim());
	    if (ok) {
	        ra.addFlashAttribute("success", true);
	        ra.addFlashAttribute("message", "Email date updated successfully.");
	    } else {
	        ra.addFlashAttribute("success", false);
	        ra.addFlashAttribute("message", "Unable to update email date.");
	    }
	    return "redirect:/online/form";
	}


}
