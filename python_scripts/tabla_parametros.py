import pandas as pd

# Función para convertir CSV a Excel
def csv_to_excel(input_csv, output_excel):
    # Leer el archivo CSV con el separador ;
    try:
        df = pd.read_csv(input_csv, sep=";")
        # Guardar como archivo Excel
        df.to_excel(output_excel, index=False, engine="openpyxl")
        print(f"Archivo Excel creado exitosamente: {output_excel}")
    except Exception as e:
        print(f"Ocurrió un error: {e}")

# Ruta del archivo CSV de entrada
input_csv = "parameter_tuning_summary.csv"  # Cambia esto por la ruta de tu archivo CSV
# Ruta del archivo Excel de salida
output_excel = "tabla.xlsx"  # Cambia esto por el nombre que desees para tu archivo Excel

# Llamar a la función
csv_to_excel(input_csv, output_excel)
