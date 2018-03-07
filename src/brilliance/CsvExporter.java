package brilliance;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import brilliance.CardDistributer.Card;

/**
 * brilliance
 * CsvExporter
 * @author Nate
 * Mar 7, 2018
 */
public class CsvExporter {

	public static final void export(List<Card> cards) {
		
		try (CSVPrinter printer = new CSVPrinter(new BufferedWriter(new FileWriter("gems.csv")), CSVFormat.DEFAULT)) {
			
			printer.printRecord("quantity", "name", "ul","ur","cl","cr","ll","lr");
			
			List<Object> values = new ArrayList<>();
			
			cards.stream().forEach(card -> {
				values.clear();
				
				//quantity
				values.add(1);
				values.add(card.id);
				Arrays.stream(card.gemSlots).forEach(gem -> {
					values.add(gem.toCSVVariable());
				});
				
				try {
					printer.printRecord(values);
				} catch (IOException e) {
					e.printStackTrace();
				}
			});
			
			
		} catch (Exception e) {
			System.out.println(e);
		}
		
	}
	
}

