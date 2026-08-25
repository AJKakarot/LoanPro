from decimal import Decimal, ROUND_HALF_UP


def _d(value: object) -> Decimal:
    if value is None or value == "":
        return Decimal("0")
    return Decimal(str(value))


def calculate_emi(principal: Decimal, annual_percent: Decimal, months: int) -> Decimal:
    if months <= 0 or principal <= 0:
        return Decimal("0.00")
    monthly_rate = (annual_percent / Decimal("1200")).quantize(Decimal("0.000000000001"))
    if monthly_rate == 0:
        return (principal / Decimal(months)).quantize(Decimal("0.01"), rounding=ROUND_HALF_UP)
    one_plus_r = Decimal("1") + monthly_rate
    power = one_plus_r ** months
    numerator = principal * monthly_rate * power
    denominator = power - Decimal("1")
    if denominator == 0:
        return Decimal("0.00")
    return (numerator / denominator).quantize(Decimal("0.01"), rounding=ROUND_HALF_UP)


def financial_metrics(application: dict) -> dict:
    principal = _d(application.get("requestedAmount"))
    rate = _d(application.get("interestRate"))
    tenure = int(application.get("tenureMonths") or 0)
    income = _d(application.get("monthlyIncome"))
    other_income = _d(application.get("otherIncome"))
    existing_emis = _d(application.get("existingEmis"))
    expenses = _d(application.get("monthlyExpenses"))
    total_income = income + other_income
    emi = calculate_emi(principal, rate, tenure)
    proposed_debt = existing_emis + emi
    dti = Decimal("0")
    if total_income > 0:
        dti = (proposed_debt / total_income).quantize(Decimal("0.0001"), rounding=ROUND_HALF_UP)
    disposable = total_income - existing_emis - expenses - emi
    return {
        "estimatedEmi": float(emi),
        "debtToIncomeRatio": float(dti),
        "disposableIncome": float(disposable.quantize(Decimal("0.01"), rounding=ROUND_HALF_UP)),
        "totalMonthlyIncome": float(total_income),
        "existingEmis": float(existing_emis),
        "requestedAmount": float(principal),
        "tenureMonths": tenure,
        "employmentType": str(application.get("employmentType") or ""),
    }
