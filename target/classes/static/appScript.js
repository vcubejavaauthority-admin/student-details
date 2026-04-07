/**
 * 
 */

function showLoading() {
	const overlay = document.getElementById('loader');
	if (overlay) {
		overlay.style.display = 'flex';
	}
}

function clearForm() {
	document.querySelector('select[name="batch"]').value = '';
	document.querySelector('input[name="date"]').value = '';
	document.querySelector('textarea[name="rollNoSet"]').value = '';
}

// helper: today (yyyy-MM-dd) string
function getTodayStr() {
	const d = new Date();
	const m = String(d.getMonth() + 1).padStart(2, '0');
	const day = String(d.getDate()).padStart(2, '0');
	return d.getFullYear() + '-' + m + '-' + day;
}

// fields change aina prathi sari validate
function setupValidation() {
	const form = document.getElementById('aemcForm');
	const batchSelect = form.querySelector('select[name="batch"]');
	const dateInput = document.getElementById('dateInput');
	const rollInput = document.getElementById('rollInput');
	const submitBtn = document.getElementById('submitBtn');

	function validateForm() {
		const batchOk = batchSelect.value.trim() !== '';
		const rollsOk = rollInput.value.trim() !== '';

		let dateOk = true;
		const val = dateInput.value;
		if (val) {
			const today = getTodayStr();
			if (val > today) {                // future date
				dateOk = false;
				dateInput.classList.add('is-invalid');
			} else {
				dateInput.classList.remove('is-invalid');
			}
		}

		const allOk = batchOk && rollsOk && dateOk;
		submitBtn.disabled = !allOk;
	}

	batchSelect.addEventListener('change', validateForm);
	rollInput.addEventListener('input', validateForm);
	dateInput.addEventListener('change', validateForm);

	// optional: invalid feedback text
	dateInput.insertAdjacentHTML('afterend',
		'<div class="invalid-feedback">Tomorrow or future date not allowed.</div>');
}

// submit time: final check + button disable + loader show
function handleSubmit(event) {
	const form = document.getElementById('aemcForm');
	const submitBtn = document.getElementById('submitBtn');
	const dateInput = document.getElementById('dateInput');

	// final future-date check
	const val = dateInput.value;
	if (val) {
		const today = getTodayStr();
		if (val > today) {
			event.preventDefault();
			dateInput.classList.add('is-invalid');
			return false;
		}
	}

	// one-click only
	submitBtn.disabled = true;
	showLoading();        // existing loader function
	return true;          // allow submit
}

// page load
document.addEventListener('DOMContentLoaded', setupValidation);
